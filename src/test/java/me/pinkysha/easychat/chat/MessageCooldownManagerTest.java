package me.pinkysha.easychat.chat;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static me.pinkysha.easychat.TestAssertions.assertEquals;
import static me.pinkysha.easychat.TestAssertions.assertFalse;
import static me.pinkysha.easychat.TestAssertions.assertTrue;

public final class MessageCooldownManagerTest {

    public static void runAll() {
        new MessageCooldownManagerTest().testCooldownFlow();
    }

    @Test
    void testCooldownFlow() {
        MessageCooldownManager manager = new MessageCooldownManager();
        UUID player = UUID.randomUUID();

        manager.setCooldownSeconds(3);

        // First message should be allowed
        assertTrue(manager.tryConsume(player));

        // Immediate next message should be blocked by cooldown
        assertFalse(manager.tryConsume(player));
        assertTrue(manager.remainingSeconds(player) > 0);

        // Clearing cooldown allows immediate sending
        manager.clear(player);
        assertEquals(0L, manager.remainingSeconds(player));
        assertTrue(manager.tryConsume(player));
    }
}
