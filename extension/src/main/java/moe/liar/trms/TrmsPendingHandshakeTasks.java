package moe.liar.trms;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded, expiry-aware task store for configuration-phase nonce challenges.
 *
 * <p>Horizon deliberately exposes no configuration-task disconnect callback.
 * The bound therefore remains necessary even when no later connection arrives
 * to trigger expiry cleanup.</p>
 */
final class TrmsPendingHandshakeTasks<T> {
    private final int maximumEntries;
    private final Map<UUID, Pending<T>> pendingByNonce = new HashMap<>();

    TrmsPendingHandshakeTasks(int maximumEntries) {
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("maximumEntries must be positive: " + maximumEntries);
        }
        this.maximumEntries = maximumEntries;
    }

    synchronized boolean add(UUID nonce, T task, long expiresAtNanos, long nowNanos) {
        Objects.requireNonNull(nonce, "nonce");
        Objects.requireNonNull(task, "task");
        discardExpired(nowNanos);
        if (pendingByNonce.size() >= maximumEntries || pendingByNonce.containsKey(nonce)) {
            return false;
        }
        pendingByNonce.put(nonce, new Pending<>(task, expiresAtNanos));
        return true;
    }

    synchronized Optional<T> remove(UUID nonce, long nowNanos) {
        Objects.requireNonNull(nonce, "nonce");
        discardExpired(nowNanos);
        Pending<T> removed = pendingByNonce.remove(nonce);
        return removed == null ? Optional.empty() : Optional.of(removed.task());
    }

    synchronized int size(long nowNanos) {
        discardExpired(nowNanos);
        return pendingByNonce.size();
    }

    private void discardExpired(long nowNanos) {
        pendingByNonce.entrySet().removeIf(entry -> nowNanos - entry.getValue().expiresAtNanos() >= 0L);
    }

    private record Pending<T>(T task, long expiresAtNanos) {
    }
}
