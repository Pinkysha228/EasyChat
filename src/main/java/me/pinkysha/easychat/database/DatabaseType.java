package me.pinkysha.easychat.database;

public enum DatabaseType {
    SQLITE,
    MYSQL,
    MARIADB;

    public static DatabaseType from(String value) {
        if (value == null) {
            return SQLITE;
        }
        return switch (value.trim().toLowerCase()) {
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            default -> SQLITE;
        };
    }
}
