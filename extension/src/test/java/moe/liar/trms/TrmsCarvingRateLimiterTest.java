package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrmsCarvingRateLimiterTest {
    @Test
    void acquiresTheFirstCarveAndThenOnlyOneCarvePerPlayerTick() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter();
        UUID playerId = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(playerId, 40L));
        assertFalse(limiter.tryAcquire(playerId, 40L));
        assertTrue(limiter.tryAcquire(playerId, 41L));
    }

    @Test
    void keepsDifferentPlayersIndependentWithinTheSameTick() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter();
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(firstPlayer, 80L));

        assertFalse(limiter.tryAcquire(firstPlayer, 80L));
        assertTrue(limiter.tryAcquire(secondPlayer, 80L));
    }

    @Test
    void reclaimsExpiredSlotsBeforeAcceptingANewPlayer() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter(1);
        UUID idlePlayer = UUID.randomUUID();
        UUID laterPlayer = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(idlePlayer, 0L));
        assertTrue(limiter.tryAcquire(laterPlayer, 20L * 60L * 5L + 20L * 30L + 1L));

        assertEquals(1, limiter.trackedPlayerCount());
        assertFalse(limiter.tryAcquire(idlePlayer, 20L * 60L * 5L + 20L * 30L + 1L));
    }

    @Test
    void rejectsNewPlayersWhenTheFixedTrackingCapacityIsFull() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter(2);
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        UUID thirdPlayer = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(firstPlayer, 80L));
        assertTrue(limiter.tryAcquire(secondPlayer, 80L));
        assertFalse(limiter.tryAcquire(thirdPlayer, 80L));
        assertFalse(limiter.tryAcquire(firstPlayer, 80L));
        assertTrue(limiter.tryAcquire(firstPlayer, 81L));
    }

    @Test
    void rejectsNegativeGameTicks() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter();
        UUID playerId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(playerId, -1L));
    }

    @Test
    void rejectsNonPositiveTrackingCapacities() {
        assertThrows(IllegalArgumentException.class, () -> new TrmsCarvingRateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new TrmsCarvingRateLimiter(-1));
    }

    @Test
    void removesPlayerAndReclaimsTrackingCapacity() {
        TrmsCarvingRateLimiter limiter = new TrmsCarvingRateLimiter(1);
        UUID playerId = UUID.randomUUID();
        UUID replacement = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(playerId, 40L));
        assertTrue(limiter.remove(playerId));
        assertEquals(0, limiter.trackedPlayerCount());
        assertTrue(limiter.tryAcquire(replacement, 40L));
        assertFalse(limiter.remove(playerId));
    }
}
