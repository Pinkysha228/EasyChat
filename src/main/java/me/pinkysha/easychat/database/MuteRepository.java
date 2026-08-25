package me.pinkysha.easychat.database;

import me.pinkysha.easychat.moderation.Mute;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class MuteRepository {
    private final DatabaseManager database;
    public MuteRepository(DatabaseManager database) { this.database = database; }

    public List<Mute> findActiveMutes() throws SQLException {
        List<Mute> result = new ArrayList<>();
        String sql = "SELECT player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at FROM mutes WHERE active = 1";
        try (PreparedStatement ps = database.connection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(read(rs));
        }
        return result;
    }

    public void save(Mute mute) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO mutes(player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, 1)")) {
            ps.setString(1, mute.playerUuid().toString());
            if (mute.moderatorUuid() == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, mute.moderatorUuid().toString());
            ps.setInt(3, mute.moderatorWeight());
            ps.setString(4, mute.reason());
            ps.setLong(5, mute.startedAt().toEpochMilli());
            if (mute.expiresAt() == null) ps.setNull(6, Types.INTEGER); else ps.setLong(6, mute.expiresAt().toEpochMilli());
            ps.executeUpdate();
        }
        if (mute.moderatorUuid() != null) upsertModerator(mute.moderatorUuid(), mute.moderatorWeight());
    }

    public void deactivate(UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement("UPDATE mutes SET active = 0 WHERE player_uuid = ? AND active = 1")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    private void upsertModerator(UUID uuid, int weight) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO moderators(moderator_uuid, group_weight, updated_at) VALUES (?, ?, ?) ON CONFLICT(moderator_uuid) DO UPDATE SET group_weight=excluded.group_weight, updated_at=excluded.updated_at")) {
            ps.setString(1, uuid.toString()); ps.setInt(2, weight); ps.setLong(3, System.currentTimeMillis()); ps.executeUpdate();
        }
    }

    private Mute read(ResultSet rs) throws SQLException {
        UUID player = UUID.fromString(rs.getString(1));
        String moderatorRaw = rs.getString(2);
        UUID moderator = moderatorRaw == null ? null : UUID.fromString(moderatorRaw);
        int weight = rs.getInt(3);
        String reason = rs.getString(4);
        Instant started = Instant.ofEpochMilli(rs.getLong(5));
        long expires = rs.getLong(6);
        Instant expiresAt = rs.wasNull() ? null : Instant.ofEpochMilli(expires);
        return new Mute(player, moderator, weight, reason, started, expiresAt);
    }
}
