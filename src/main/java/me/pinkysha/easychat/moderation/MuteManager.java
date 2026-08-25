package me.pinkysha.easychat.moderation;

import me.pinkysha.easychat.EasyChat;
import me.pinkysha.easychat.database.MuteRepository;
import me.pinkysha.easychat.group.GroupWeightProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MuteManager {
    private final EasyChat plugin;
    private final MuteRepository repository;
    private final GroupWeightProvider weightProvider;
    private final Map<UUID, Mute> active = new ConcurrentHashMap<>();

    public MuteManager(EasyChat plugin, MuteRepository repository, GroupWeightProvider weightProvider) {
        this.plugin = plugin;
        this.repository = repository;
        this.weightProvider = weightProvider;
    }

    public CompletableFuture<List<Mute>> load() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                active.clear();
                List<Mute> loaded = repository.findActiveMutes();
                Instant now = Instant.now();
                for (Mute mute : loaded) {
                    if (!mute.isExpired(now)) {
                        active.put(mute.playerUuid(), mute);
                    }
                }
                return loaded;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load mutes: " + e.getMessage());
                return List.of();
            }
        }, plugin.asyncExecutor());
    }

    public Mute get(UUID uuid) {
        return active.get(uuid);
    }

    public boolean isMuted(UUID uuid) {
        Mute mute = active.get(uuid);
        return mute != null && !mute.isExpired(Instant.now());
    }

    public List<String> getActiveMuteNames() {
        List<String> result = new ArrayList<>();
        for (UUID uuid : active.keySet()) {
            String name = getPlayerName(uuid);
            if (name != null) result.add(name);
        }
        return Collections.unmodifiableList(result);
    }

    public String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    public Mute getByName(String name) {
        for (Map.Entry<UUID, Mute> entry : active.entrySet()) {
            String playerName = getPlayerName(entry.getKey());
            if (playerName != null && playerName.equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public CompletableFuture<Mute> mute(CommandSender moderator, UUID target, java.time.Duration duration, String reason) {
        UUID moderatorUuid = moderator instanceof Player player ? player.getUniqueId() : null;
        int weight = moderator instanceof ConsoleCommandSender ? Integer.MAX_VALUE : weightProvider.getWeight(moderatorUuid);
        Instant startedAt = Instant.now();
        Mute mute = new Mute(target, moderatorUuid, weight, reason, startedAt,
                duration == null ? null : startedAt.plus(duration));

        active.put(target, mute);
        return CompletableFuture.runAsync(() -> {
            try {
                repository.save(mute);
            } catch (Exception e) {
                active.remove(target, mute);
                throw new IllegalStateException("Failed to save mute", e);
            }
        }, plugin.asyncExecutor()).thenApply(v -> mute);
    }

    public CompletableFuture<Boolean> unmute(UUID target) {
        active.remove(target);
        return CompletableFuture.supplyAsync(() -> {
            try {
                repository.deactivate(target);
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to remove mute: " + e.getMessage());
                return false;
            }
        }, plugin.asyncExecutor());
    }

    public boolean canUnmute(UUID moderatorUuid, Mute mute) {
        return weightProvider.getWeight(moderatorUuid) >= mute.moderatorWeight();
    }
}
