package me.pinkysha.easychat.chat;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static me.pinkysha.easychat.TestAssertions.*;

public final class PrivateMessageManagerTest {

    public static void runAll() {
        new PrivateMessageManagerTest().testReplyTargetTracking();
        new PrivateMessageManagerTest().testToggleMessages();
        new PrivateMessageManagerTest().testIgnoreToggle();
    }

    @Test
    void testReplyTargetTracking() {
        PrivateMessageManager manager = new PrivateMessageManager(null, null, null);
        UUID player1 = UUID.randomUUID();

        assertNull(manager.getReplyTarget(player1));

        manager.setReplyTarget(player1, "TargetPlayer");
        assertEquals("TargetPlayer", manager.getReplyTarget(player1));
    }

    @Test
    void testToggleMessages() {
        PrivateMessageManager manager = new PrivateMessageManager(null, null, null);
        UUID player1 = UUID.randomUUID();

        assertFalse(manager.isToggledOff(player1));

        // Toggle off
        boolean state1 = manager.toggleMessages(player1);
        assertFalse(state1);
        assertTrue(manager.isToggledOff(player1));

        // Toggle on
        boolean state2 = manager.toggleMessages(player1);
        assertTrue(state2);
        assertFalse(manager.isToggledOff(player1));
    }

    @Test
    void testIgnoreToggle() {
        PrivateMessageManager manager = new PrivateMessageManager(null, null, null);
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        assertFalse(manager.isIgnoring(player1, player2));

        // Ignore player2
        boolean ignored = manager.toggleIgnore(player1, player2);
        assertTrue(ignored);
        assertTrue(manager.isIgnoring(player1, player2));

        // Unignore player2
        boolean unignored = manager.toggleIgnore(player1, player2);
        assertFalse(unignored);
        assertFalse(manager.isIgnoring(player1, player2));
    }
}
