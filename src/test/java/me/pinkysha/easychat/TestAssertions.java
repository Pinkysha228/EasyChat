package me.pinkysha.easychat;

import java.util.Objects;

public final class TestAssertions {
    private TestAssertions() {}

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertTrue(boolean condition) {
        assertTrue(condition, "Expected condition to be true");
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, "Expected condition to be false");
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    public static void assertNull(Object object) {
        if (object != null) {
            throw new AssertionError("Expected null but was: <" + object + ">");
        }
    }

    public static void assertNotNull(Object object) {
        if (object == null) {
            throw new AssertionError("Expected non-null object");
        }
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, Runnable executable) {
        try {
            executable.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError("Expected exception " + expectedType.getName() + " but caught " + t.getClass().getName(), t);
        }
        throw new AssertionError("Expected exception " + expectedType.getName() + " but nothing was thrown");
    }
}
