package me.pinkysha.easychat.chat;

import org.junit.jupiter.api.Test;

import static me.pinkysha.easychat.TestAssertions.assertEquals;
import static me.pinkysha.easychat.TestAssertions.assertNull;

public final class ChatColorManagerTest {

    public static void runAll() {
        new ChatColorManagerTest().testNormalizeInput();
        new ChatColorManagerTest().testDisplayName();
    }

    @Test
    void testNormalizeInput() {
        ChatColorManager manager = new ChatColorManager(null, null);

        assertEquals("&#ff0000", manager.normalizeColorInput("&#ff0000"));
        assertEquals("&#ff0000", manager.normalizeColorInput("#ff0000"));
        assertEquals("&#ff0000", manager.normalizeColorInput("ff0000"));
        assertEquals("&a", manager.normalizeColorInput("&a"));
        assertEquals("&a", manager.normalizeColorInput("a"));
        assertEquals("&c", manager.normalizeColorInput("&C"));
        assertNull(manager.normalizeColorInput("invalid"));
        assertNull(manager.normalizeColorInput(""));
        assertNull(manager.normalizeColorInput(null));
    }

    @Test
    void testDisplayName() {
        ChatColorManager manager = new ChatColorManager(null, null);

        assertEquals("Green", manager.getColorDisplayName("&a"));
        assertEquals("Red", manager.getColorDisplayName("&c"));
        assertEquals("Gold", manager.getColorDisplayName("&6"));
        assertEquals("&#FFAA00", manager.getColorDisplayName("&#ffaa00"));
        assertEquals("Default", manager.getColorDisplayName(null));
        assertEquals("Default", manager.getColorDisplayName(""));
    }
}
