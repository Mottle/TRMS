package moe.liar.trms;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Thread-safe, server-tick-based lifecycle for one player's assembly preview.
 *
 * <p>The network handlers run on player-owned region threads while quit/death
 * and server-tick callbacks can run on different Horizon contexts. This class
 * therefore owns all cross-context state and never stores a Minecraft object
 * outside the short-lived session value supplied by the caller.</p>
 */
final class TrmsWeaponAssemblySessions<T> {
    private final long sessionTicks;
    private final long startCooldownTicks;
    private final Map<UUID, Session<T>> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastStartTicks = new ConcurrentHashMap<>();
    private final Semaphore availableSessionSlots;

    TrmsWeaponAssemblySessions(long sessionTicks, long startCooldownTicks, int maxSessions) {
        if (sessionTicks <= 0 || startCooldownTicks < 0 || maxSessions <= 0) {
            throw new IllegalArgumentException("Invalid assembly session limits");
        }
        this.sessionTicks = sessionTicks;
        this.startCooldownTicks = startCooldownTicks;
        this.availableSessionSlots = new Semaphore(maxSessions);
    }

    BeginResult<T> begin(UUID playerId, int entityId, T input, long nowTick) {
        purgeExpired(nowTick);
        Session<T> active = sessions.get(playerId);
        if (active != null) {
            return new BeginResult<>(BeginStatus.ALREADY_ACTIVE, active);
        }
        Long lastStart = lastStartTicks.get(playerId);
        if (lastStart != null && nowTick - lastStart < startCooldownTicks) {
            return new BeginResult<>(BeginStatus.RATE_LIMITED, null);
        }
        if (!availableSessionSlots.tryAcquire()) {
            return new BeginResult<>(BeginStatus.CAPACITY_REACHED, null);
        }
        Session<T> candidate = new Session<>(UUID.randomUUID(), entityId, input, nowTick + sessionTicks);
        Session<T> previous = sessions.putIfAbsent(playerId, candidate);
        if (previous != null) {
            availableSessionSlots.release();
            return new BeginResult<>(BeginStatus.ALREADY_ACTIVE, previous);
        }
        lastStartTicks.put(playerId, nowTick);
        return new BeginResult<>(BeginStatus.CREATED, candidate);
    }

    Session<T> get(UUID playerId) {
        return sessions.get(playerId);
    }

    boolean remove(UUID playerId, UUID sessionId) {
        Session<T> current = sessions.get(playerId);
        if (current == null || !current.id().equals(sessionId)
                || !sessions.remove(playerId, current)) {
            return false;
        }
        availableSessionSlots.release();
        return true;
    }

    boolean remove(UUID playerId) {
        if (sessions.remove(playerId) == null) {
            return false;
        }
        availableSessionSlots.release();
        return true;
    }

    int purgeExpired(long nowTick) {
        int removed = 0;
        for (Map.Entry<UUID, Session<T>> entry : sessions.entrySet()) {
            if (entry.getValue().expiresAt() <= nowTick && sessions.remove(entry.getKey(), entry.getValue())) {
                removed++;
                availableSessionSlots.release();
            }
        }
        for (Map.Entry<UUID, Long> entry : lastStartTicks.entrySet()) {
            if (nowTick - entry.getValue() >= sessionTicks) {
                lastStartTicks.remove(entry.getKey(), entry.getValue());
            }
        }
        return removed;
    }

    int size() {
        return sessions.size();
    }

    enum BeginStatus {
        CREATED,
        ALREADY_ACTIVE,
        RATE_LIMITED,
        CAPACITY_REACHED
    }

    record BeginResult<T>(BeginStatus status, Session<T> session) {
        boolean created() {
            return status == BeginStatus.CREATED;
        }
    }

    record Session<T>(UUID id, int entityId, T input, long expiresAt) {
    }
}
