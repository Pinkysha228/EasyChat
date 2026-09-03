package me.pinkysha.easychat.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UserSettingsRepository {
    private final DatabaseManager database;

    public UserSettingsRepository(DatabaseManager database) {
        this.database = database;
    }

    public Map<UUID, String> loadAllChatColors() throws SQLException {
        Map<UUID, String> colors = new HashMap<>();
        String sql = "SELECT player_uuid, chat_color FROM user_settings";
        try (Connection conn = database.connection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String color = rs.getString("chat_color");
                colors.put(uuid, color);
            }
        }
        return colors;
    }

    public void saveChatColor(UUID playerUuid, String chatColor) throws SQLException {
        String sql;
        if (database.isOffline()) {
            sql = "INSERT INTO user_settings(player_uuid, chat_color, updated_at) "
                    + "VALUES (?, ?, ?) "
                    + "ON CONFLICT(player_uuid) DO UPDATE SET "
                    + "chat_color = excluded.chat_color, updated_at = excluded.updated_at";
        } else {
            sql = "INSERT INTO user_settings(player_uuid, chat_color, updated_at) "
                    + "VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "chat_color = VALUES(chat_color), updated_at = VALUES(updated_at)";
        }

        try (Connection conn = database.connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, chatColor);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void removeChatColor(UUID playerUuid) throws SQLException {
        String sql = "DELETE FROM user_settings WHERE player_uuid = ?";
        try (Connection conn = database.connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        }
    }
}
