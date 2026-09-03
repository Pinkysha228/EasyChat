package me.pinkysha.easychat.util;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static me.pinkysha.easychat.TestAssertions.*;

public final class DurationParserTest {

    public static void runAll() {
        new DurationParserTest().testPermanentDurations();
        new DurationParserTest().testSeconds();
        new DurationParserTest().testMinutes();
        new DurationParserTest().testHours();
        new DurationParserTest().testDays();
        new DurationParserTest().testWeeks();
        new DurationParserTest().testInvalidFormats();
    }

    @Test
    void testPermanentDurations() {
        assertNull(DurationParser.parse(null));
        assertNull(DurationParser.parse("perm"));
        assertNull(DurationParser.parse("permanent"));
        assertNull(DurationParser.parse("PERM"));
        assertNull(DurationParser.parse("PERMANENT"));
    }

    @Test
    void testSeconds() {
        Duration duration = DurationParser.parse("45s");
        assertNotNull(duration);
        assertEquals(45L, duration.getSeconds());
    }

    @Test
    void testMinutes() {
        Duration duration = DurationParser.parse("10m");
        assertNotNull(duration);
        assertEquals(600L, duration.getSeconds());
    }

    @Test
    void testHours() {
        Duration duration = DurationParser.parse("2h");
        assertNotNull(duration);
        assertEquals(7200L, duration.getSeconds());
    }

    @Test
    void testDays() {
        Duration duration = DurationParser.parse("7d");
        assertNotNull(duration);
        assertEquals(7L * 86400L, duration.getSeconds());
    }

    @Test
    void testWeeks() {
        Duration duration = DurationParser.parse("2w");
        assertNotNull(duration);
        assertEquals(2L * 7L * 86400L, duration.getSeconds());
    }

    @Test
    void testInvalidFormats() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10x"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("m10"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("-1"));
    }
}
