package me.pinkysha.easychat.moderation;

import java.time.Instant;
import java.util.UUID;

public record Mute(UUID playerUuid, UUID moderatorUuid, int moderatorWeight, String reason, Instant startedAt, Instant expiresAt) {
    public boolean isPermanent() { return expiresAt == null; }
    public boolean isExpired(Instant now) { return expiresAt != null && !expiresAt.isAfter(now); }
}
