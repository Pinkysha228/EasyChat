package me.pinkysha.easychat.moderation;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

import static me.pinkysha.easychat.TestAssertions.assertFalse;
import static me.pinkysha.easychat.TestAssertions.assertTrue;

public final class MuteTest {

    public static void runAll() {
        new MuteTest().testPermanentMuteNeverExpires();
        new MuteTest().testTemporaryMuteExpiration();
    }

    @Test
    void testPermanentMuteNeverExpires() {
        Mute mute = new Mute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100,
                "spam",
                Instant.now(),
                null
        );

        assertFalse(mute.isExpired(Instant.now()));
        assertFalse(mute.isExpired(Instant.now().plusSeconds(1000000)));
    }

    @Test
    void testTemporaryMuteExpiration() {
        Instant now = Instant.now();
        Mute mute = new Mute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                50,
                "caps",
                now.minusSeconds(60),
                now.minusSeconds(10)
        );

        assertTrue(mute.isExpired(now));

        Mute futureMute = new Mute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                50,
                "caps",
                now,
                now.plusSeconds(300)
        );

        assertFalse(futureMute.isExpired(now));
    }
}
