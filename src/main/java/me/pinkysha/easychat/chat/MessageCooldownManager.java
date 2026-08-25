package me.pinkysha.easychat.chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageCooldownManager {
    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();
    private volatile long cooldownMillis;

    public void setCooldownSeconds(long seconds) { cooldownMillis = Math.max(0, seconds) * 1000L; }
    public long remainingSeconds(UUID uuid) {
        if (cooldownMillis <= 0) return 0;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastMessage.getOrDefault(uuid, 0L));
        return Math.max(0, (long) Math.ceil(remaining / 1000.0));
    }
    public boolean tryConsume(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastMessage.getOrDefault(uuid, 0L);
        if (cooldownMillis > 0 && now - last < cooldownMillis) return false;
        lastMessage.put(uuid, now);
        return true;
    }
    public void clear(UUID uuid) { lastMessage.remove(uuid); }
}
