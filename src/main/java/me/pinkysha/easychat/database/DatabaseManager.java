package me.pinkysha.easychat.database;

import me.pinkysha.easychat.EasyChat;

import java.io.File;
import java.sql.*;

public final class DatabaseManager {
    private final EasyChat plugin;
    private Connection connection;

    public DatabaseManager(EasyChat plugin) { this.plugin = plugin; }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        File db = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.file", "easychat.db"));
        if (!db.getParentFile().exists() && !db.getParentFile().mkdirs()) {
            throw new SQLException("Could not create plugin data folder");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA foreign_keys=ON");
        }
        migrate();
    }

    private void migrate() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS mutes (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, moderator_uuid TEXT NULL, moderator_weight INTEGER NOT NULL, reason TEXT NOT NULL, started_at INTEGER NOT NULL, expires_at INTEGER NULL, active INTEGER NOT NULL DEFAULT 1)");
            boolean moderatorUuidNotNull = false;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(mutes)")) {
                while (rs.next()) {
                    if ("moderator_uuid".equalsIgnoreCase(rs.getString("name"))) moderatorUuidNotNull = rs.getInt("notnull") == 1;
                }
            }
            if (moderatorUuidNotNull) {
                st.executeUpdate("ALTER TABLE mutes RENAME TO mutes_legacy");
                st.executeUpdate("CREATE TABLE mutes (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, moderator_uuid TEXT NULL, moderator_weight INTEGER NOT NULL, reason TEXT NOT NULL, started_at INTEGER NOT NULL, expires_at INTEGER NULL, active INTEGER NOT NULL DEFAULT 1)");
                st.executeUpdate("INSERT INTO mutes(id, player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active) SELECT id, player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active FROM mutes_legacy");
                st.executeUpdate("DROP TABLE mutes_legacy");
            }
            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_active_mute_player ON mutes(player_uuid) WHERE active = 1");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS moderators (moderator_uuid TEXT PRIMARY KEY, group_weight INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        }
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        return connection;
    }

    public synchronized void close() {
        if (connection == null) return;
        try { connection.close(); } catch (SQLException ignored) {}
        connection = null;
    }
}
