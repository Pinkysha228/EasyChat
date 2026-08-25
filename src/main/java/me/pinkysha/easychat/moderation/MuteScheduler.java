package me.pinkysha.easychat.moderation;

import me.pinkysha.easychat.EasyChat;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuteScheduler {
    private final EasyChat plugin;
    private final MuteManager muteManager;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public MuteScheduler(EasyChat plugin, MuteManager muteManager) { this.plugin=plugin;this.muteManager=muteManager; }

    public void scheduleAll(java.util.Collection<Mute> mutes) { mutes.forEach(m -> schedule(m.playerUuid(), m.expiresAt())); }

    public void schedule(UUID uuid) {
        Mute mute = muteManager.get(uuid);
        if (mute != null && mute.expiresAt() != null) schedule(uuid, mute.expiresAt());
    }

    public void schedule(UUID uuid, Instant expiresAt) {
        cancel(uuid);
        if (expiresAt == null) return;
        long millis = Math.max(1, Duration.between(Instant.now(), expiresAt).toMillis());
        long ticks = Math.max(1, (millis + 49) / 50);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(uuid);
            Mute mute = muteManager.get(uuid);
            if (mute != null && mute.isExpired(Instant.now())) {
                String playerName = muteManager.getPlayerName(uuid);
                muteManager.unmute(uuid).thenAccept(ok -> {
                    if (ok) {
                        plugin.getLogger().info("[UNMUTE] Automatic unmute for " + (playerName == null ? uuid : playerName)
                                + " after mute expiration.");
                    }
                });
            } else if (mute != null) {
                schedule(uuid, mute.expiresAt());
            }
        }, ticks);
        tasks.put(uuid, task);
    }

    public void cancel(UUID uuid) { BukkitTask task = tasks.remove(uuid); if (task != null) task.cancel(); }
    public void cancelAll() { tasks.values().forEach(BukkitTask::cancel); tasks.clear(); }
}
