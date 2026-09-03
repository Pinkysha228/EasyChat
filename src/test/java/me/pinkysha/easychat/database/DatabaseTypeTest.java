package me.pinkysha.easychat.database;

import org.junit.jupiter.api.Test;

import static me.pinkysha.easychat.TestAssertions.assertEquals;

public final class DatabaseTypeTest {

    public static void runAll() {
        new DatabaseTypeTest().testFromValidValues();
        new DatabaseTypeTest().testFromUnknownOrNull();
    }

    @Test
    void testFromValidValues() {
        assertEquals(DatabaseType.MYSQL, DatabaseType.from("mysql"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.from("MySQL"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.from(" MYSQL "));
        assertEquals(DatabaseType.MARIADB, DatabaseType.from("mariadb"));
        assertEquals(DatabaseType.MARIADB, DatabaseType.from("MariaDB"));
        assertEquals(DatabaseType.SQLITE, DatabaseType.from("sqlite"));
        assertEquals(DatabaseType.SQLITE, DatabaseType.from("SQLite"));
    }

    @Test
    void testFromUnknownOrNull() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.from(null));
        assertEquals(DatabaseType.SQLITE, DatabaseType.from(""));
        assertEquals(DatabaseType.SQLITE, DatabaseType.from("unknown"));
        assertEquals(DatabaseType.SQLITE, DatabaseType.from("postgres"));
    }
}
