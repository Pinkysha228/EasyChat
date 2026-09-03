package me.pinkysha.easychat.database;

import me.pinkysha.easychat.EasyChat;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public final class DatabaseManager {
    private final EasyChat plugin;
    private Connection connection;
    private DatabaseType type = DatabaseType.SQLITE;

    public DatabaseManager(EasyChat plugin) {
        this.plugin = plugin;
    }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed() && connection.isValid(2)) {
            return;
        }

        boolean isOffline = plugin.getConfig().getBoolean("database.is-offline",
                plugin.getConfig().getBoolean("database.is_offline", true));

        if (isOffline) {
            this.type = DatabaseType.SQLITE;
            File db = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.file", "easychat.db"));
            if (!db.getParentFile().exists() && !db.getParentFile().mkdirs()) {
                throw new SQLException("Could not create plugin data folder");
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        } else {
            String rawType = plugin.getConfig().getString("database.type", "mysql");
            this.type = DatabaseType.from(rawType);

            String host = plugin.getConfig().getString("database.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String dbName = plugin.getConfig().getString("database.database", "easychat");
            String username = plugin.getConfig().getString("database.username", "root");
            String password = plugin.getConfig().getString("database.password", "");
            boolean ssl = plugin.getConfig().getBoolean("database.ssl", false);

            String url;
            if (this.type == DatabaseType.MARIADB) {
                url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName
                        + "?useSsl=" + ssl + "&characterEncoding=utf8&autoReconnect=true";
            } else {
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                        + "?useSSL=" + ssl + "&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true";
            }

            connection = DriverManager.getConnection(url, username, password);
        }

        initTables();
    }

    private void initTables() throws SQLException {
        if (type == DatabaseType.SQLITE) {
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA foreign_keys=ON");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS mutes ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "player_uuid VARCHAR(36) NOT NULL, "
                        + "moderator_uuid VARCHAR(36) NULL, "
                        + "moderator_weight INTEGER NOT NULL, "
                        + "reason TEXT NOT NULL, "
                        + "started_at BIGINT NOT NULL, "
                        + "expires_at BIGINT NULL, "
                        + "active INTEGER NOT NULL DEFAULT 1)");

                boolean moderatorUuidNotNull = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(mutes)")) {
                    while (rs.next()) {
                        if ("moderator_uuid".equalsIgnoreCase(rs.getString("name"))) {
                            moderatorUuidNotNull = rs.getInt("notnull") == 1;
                        }
                    }
                }

                if (moderatorUuidNotNull) {
                    st.executeUpdate("ALTER TABLE mutes RENAME TO mutes_legacy");
                    st.executeUpdate("CREATE TABLE mutes ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "player_uuid VARCHAR(36) NOT NULL, "
                            + "moderator_uuid VARCHAR(36) NULL, "
                            + "moderator_weight INTEGER NOT NULL, "
                            + "reason TEXT NOT NULL, "
                            + "started_at BIGINT NOT NULL, "
                            + "expires_at BIGINT NULL, "
                            + "active INTEGER NOT NULL DEFAULT 1)");
                    st.executeUpdate("INSERT INTO mutes(id, player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active) "
                            + "SELECT id, player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active FROM mutes_legacy");
                    st.executeUpdate("DROP TABLE mutes_legacy");
                }

                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mutes_player_active ON mutes(player_uuid, active)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS moderators ("
                        + "moderator_uuid VARCHAR(36) PRIMARY KEY, "
                        + "group_weight INTEGER NOT NULL, "
                        + "updated_at BIGINT NOT NULL)");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS user_settings ("
                        + "player_uuid VARCHAR(36) PRIMARY KEY, "
                        + "chat_color VARCHAR(32) NOT NULL, "
                        + "updated_at BIGINT NOT NULL)");
            }
        } else {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS mutes ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "player_uuid VARCHAR(36) NOT NULL, "
                        + "moderator_uuid VARCHAR(36) NULL, "
                        + "moderator_weight INT NOT NULL, "
                        + "reason TEXT NOT NULL, "
                        + "started_at BIGINT NOT NULL, "
                        + "expires_at BIGINT NULL, "
                        + "active TINYINT NOT NULL DEFAULT 1, "
                        + "INDEX idx_mutes_player_active (player_uuid, active)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS moderators ("
                        + "moderator_uuid VARCHAR(36) PRIMARY KEY, "
                        + "group_weight INT NOT NULL, "
                        + "updated_at BIGINT NOT NULL"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS user_settings ("
                        + "player_uuid VARCHAR(36) PRIMARY KEY, "
                        + "chat_color VARCHAR(32) NOT NULL, "
                        + "updated_at BIGINT NOT NULL"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
        }
    }

    public record MigrationResult(int mutesCount, int moderatorsCount, long durationMs) {}

    public MigrationResult migrateFromSqlite(File sqliteFile) throws SQLException {
        if (isOffline()) {
            throw new IllegalStateException("Active database is offline (SQLite). Migration can only be performed to an online database (MySQL/MariaDB).");
        }
        if (!sqliteFile.exists()) {
            throw new IllegalArgumentException("SQLite database file not found: " + sqliteFile.getAbsolutePath());
        }

        long startTime = System.currentTimeMillis();
        int mutesCount = 0;
        int moderatorsCount = 0;

        String sqliteUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        Connection targetConn = connection();
        try (Connection sqliteConn = DriverManager.getConnection(sqliteUrl)) {
            boolean originalAutoCommit = targetConn.getAutoCommit();
            targetConn.setAutoCommit(false);
            try {
                String selectModsSql = "SELECT moderator_uuid, group_weight, updated_at FROM moderators";
                String insertModsSql = "INSERT INTO moderators(moderator_uuid, group_weight, updated_at) "
                        + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE group_weight = VALUES(group_weight), updated_at = VALUES(updated_at)";

                try (Statement st = sqliteConn.createStatement();
                     ResultSet rs = st.executeQuery(selectModsSql);
                     PreparedStatement ps = targetConn.prepareStatement(insertModsSql)) {
                    while (rs.next()) {
                        ps.setString(1, rs.getString("moderator_uuid"));
                        ps.setInt(2, rs.getInt("group_weight"));
                        ps.setLong(3, rs.getLong("updated_at"));
                        ps.addBatch();
                        moderatorsCount++;
                    }
                    if (moderatorsCount > 0) {
                        ps.executeBatch();
                    }
                } catch (SQLException ignored) {
                    // Moderators table might not exist in older SQLite versions
                }

                String selectMutesSql = "SELECT player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active FROM mutes";
                String insertMutesSql = "INSERT INTO mutes(player_uuid, moderator_uuid, moderator_weight, reason, started_at, expires_at, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (Statement st = sqliteConn.createStatement();
                     ResultSet rs = st.executeQuery(selectMutesSql);
                     PreparedStatement ps = targetConn.prepareStatement(insertMutesSql)) {
                    while (rs.next()) {
                        ps.setString(1, rs.getString("player_uuid"));
                        String modUuid = rs.getString("moderator_uuid");
                        if (modUuid == null) {
                            ps.setNull(2, Types.VARCHAR);
                        } else {
                            ps.setString(2, modUuid);
                        }
                        ps.setInt(3, rs.getInt("moderator_weight"));
                        ps.setString(4, rs.getString("reason"));
                        ps.setLong(5, rs.getLong("started_at"));
                        long expiresAt = rs.getLong("expires_at");
                        if (rs.wasNull()) {
                            ps.setNull(6, Types.BIGINT);
                        } else {
                            ps.setLong(6, expiresAt);
                        }
                        ps.setInt(7, rs.getInt("active"));
                        ps.addBatch();
                        mutesCount++;
                    }
                    if (mutesCount > 0) {
                        ps.executeBatch();
                    }
                }

                // Migrate user_settings
                try {
                    String selectSettingsSql = "SELECT player_uuid, chat_color, updated_at FROM user_settings";
                    String insertSettingsSql = "INSERT INTO user_settings(player_uuid, chat_color, updated_at) "
                            + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE chat_color = VALUES(chat_color), updated_at = VALUES(updated_at)";
                    try (Statement st = sqliteConn.createStatement();
                         ResultSet rs = st.executeQuery(selectSettingsSql);
                         PreparedStatement ps = targetConn.prepareStatement(insertSettingsSql)) {
                        while (rs.next()) {
                            ps.setString(1, rs.getString("player_uuid"));
                            ps.setString(2, rs.getString("chat_color"));
                            ps.setLong(3, rs.getLong("updated_at"));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (SQLException ignored) {
                    // user_settings might not exist in older SQLite versions
                }

                targetConn.commit();
            } catch (SQLException ex) {
                targetConn.rollback();
                throw ex;
            } finally {
                targetConn.setAutoCommit(originalAutoCommit);
            }
        }

        return new MigrationResult(mutesCount, moderatorsCount, System.currentTimeMillis() - startTime);
    }

    public boolean isOffline() {
        return type == DatabaseType.SQLITE;
    }

    public DatabaseType type() {
        return type;
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connect();
        }
        return connection;
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
        connection = null;
    }
}
