package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrmsPendingHandshakeTasksTest {
    @Test
    void removesAndReturnsTheTaskRegisteredForItsNonce() {
        TrmsPendingHandshakeTasks<Object> tasks = new TrmsPendingHandshakeTasks<>(2);
        UUID nonce = UUID.randomUUID();
        Object task = new Object();

        assertTrue(tasks.add(nonce, task, 200L, 100L));

        assertSame(task, tasks.remove(nonce, 150L).orElseThrow());
        assertEquals(0, tasks.size(150L));
    }

    @Test
    void treatsTasksAsExpiredAtTheirDeadline() {
        TrmsPendingHandshakeTasks<Object> tasks = new TrmsPendingHandshakeTasks<>(2);
        UUID nonce = UUID.randomUUID();

        assertTrue(tasks.add(nonce, new Object(), 200L, 100L));

        assertTrue(tasks.remove(nonce, 200L).isEmpty());
        assertEquals(0, tasks.size(200L));
    }

    @Test
    void enforcesCapacityUntilExpiredTasksAreReaped() {
        TrmsPendingHandshakeTasks<Object> tasks = new TrmsPendingHandshakeTasks<>(1);

        assertTrue(tasks.add(UUID.randomUUID(), new Object(), 200L, 100L));
        assertFalse(tasks.add(UUID.randomUUID(), new Object(), 300L, 150L));

        assertTrue(tasks.add(UUID.randomUUID(), new Object(), 400L, 200L));
        assertEquals(1, tasks.size(200L));
    }

    @Test
    void rejectsDuplicateNoncesWithoutReplacingTheOriginalTask() {
        TrmsPendingHandshakeTasks<Object> tasks = new TrmsPendingHandshakeTasks<>(2);
        UUID nonce = UUID.randomUUID();
        Object original = new Object();

        assertTrue(tasks.add(nonce, original, 300L, 100L));
        assertFalse(tasks.add(nonce, new Object(), 400L, 150L));

        assertSame(original, tasks.remove(nonce, 200L).orElseThrow());
    }

    @Test
    void requiresAPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new TrmsPendingHandshakeTasks<>(0));
        assertThrows(IllegalArgumentException.class, () -> new TrmsPendingHandshakeTasks<>(-1));
    }
}
