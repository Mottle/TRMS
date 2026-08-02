package moe.liar.trms;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;

/**
 * Server-only accepted-action gate for the carving packet.
 *
 * <p>The client may send arbitrary packet bursts. A normal player action may
 * accept at most one carve per game tick, regardless of how many valid
 * revision values a modified client predicts. Entries expire after a bounded
 * idle retention window and the table has a fixed defensive capacity, so a
 * quiet server cannot retain an unbounded player history.</p>
 */
final class TrmsCarvingRateLimiter {
    private static final long RETENTION_TICKS = 20L * 60L * 5L;
    private static final long PRUNE_INTERVAL_TICKS = 20L * 30L;
    private static final int DEFAULT_MAX_TRACKED_PLAYERS = 4_096;

    private final ConcurrentHashMap<UUID, Long> lastAcceptedTickByPlayer = new ConcurrentHashMap<>();
    private final Semaphore availablePlayerSlots;
    private final AtomicLong lastPruneTick = new AtomicLong(Long.MIN_VALUE);

    TrmsCarvingRateLimiter() {
        this(DEFAULT_MAX_TRACKED_PLAYERS);
    }

    TrmsCarvingRateLimiter(int maxTrackedPlayers) {
        if (maxTrackedPlayers <= 0) {
            throw new IllegalArgumentException("TRMS maximum tracked players must be positive: " + maxTrackedPlayers);
        }
        availablePlayerSlots = new Semaphore(maxTrackedPlayers);
    }

    /**
     * Reserves this tick's one accepted carve for a fully validated player action.
     *
     * <p>A first-seen player consumes one of a fixed number of tracking slots.
     * When every slot is live, the limiter fails closed instead of evicting a
     * player whose same-tick action must remain rejected. The normal server
     * population is far below this defensive bound.</p>
     */
    boolean tryAcquire(UUID playerId, long gameTick) {
        Objects.requireNonNull(playerId, "playerId");
        requireGameTick(gameTick);
        discardStaleEntries(gameTick);
        while (true) {
            Long lastAcceptedTick = lastAcceptedTickByPlayer.get(playerId);
            if (lastAcceptedTick == null) {
                if (!availablePlayerSlots.tryAcquire()) {
                    return false;
                }
                if (lastAcceptedTickByPlayer.putIfAbsent(playerId, gameTick) == null) {
                    return true;
                }
                availablePlayerSlots.release();
                continue;
            }
            if (lastAcceptedTick >= gameTick) {
                return false;
            }
            if (lastAcceptedTickByPlayer.replace(playerId, lastAcceptedTick, gameTick)) {
                return true;
            }
        }
    }

    int trackedPlayerCount() {
        return lastAcceptedTickByPlayer.size();
    }

    private void discardStaleEntries(long gameTick) {
        long previousPruneTick = lastPruneTick.get();
        if (previousPruneTick != Long.MIN_VALUE && gameTick - previousPruneTick < PRUNE_INTERVAL_TICKS) {
            return;
        }
        if (!lastPruneTick.compareAndSet(previousPruneTick, gameTick)) {
            return;
        }
        long oldestRetainedTick = Math.max(0L, gameTick - RETENTION_TICKS);
        for (var entry : lastAcceptedTickByPlayer.entrySet()) {
            if (entry.getValue() < oldestRetainedTick
                    && lastAcceptedTickByPlayer.remove(entry.getKey(), entry.getValue())) {
                availablePlayerSlots.release();
            }
        }
    }

    private static void requireGameTick(long gameTick) {
        if (gameTick < 0L) {
            throw new IllegalArgumentException("TRMS game tick must not be negative: " + gameTick);
        }
    }
}
