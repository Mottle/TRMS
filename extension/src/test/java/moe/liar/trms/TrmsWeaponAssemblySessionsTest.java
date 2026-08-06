package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrmsWeaponAssemblySessionsTest {
    @Test
    void repeatedStartKeepsTheExistingSession() {
        TrmsWeaponAssemblySessions<String> sessions = new TrmsWeaponAssemblySessions<>(1_200, 4, 8);
        UUID player = UUID.randomUUID();

        var first = sessions.begin(player, 7, "part", 100);
        var second = sessions.begin(player, 7, "part", 101);

        assertTrue(first.created());
        assertEquals(TrmsWeaponAssemblySessions.BeginStatus.ALREADY_ACTIVE, second.status());
        assertEquals(first.session(), second.session());
        assertEquals(1, sessions.size());
    }

    @Test
    void cancelledSessionCannotBeImmediatelyRecreated() {
        TrmsWeaponAssemblySessions<String> sessions = new TrmsWeaponAssemblySessions<>(1_200, 4, 8);
        UUID player = UUID.randomUUID();
        var first = sessions.begin(player, 7, "part", 100).session();

        assertTrue(sessions.remove(player, first.id()));
        var second = sessions.begin(player, 7, "part", 101);

        assertEquals(TrmsWeaponAssemblySessions.BeginStatus.RATE_LIMITED, second.status());
        assertFalse(sessions.remove(player, first.id()));
    }

    @Test
    void expirationUsesTheSuppliedGlobalTick() {
        TrmsWeaponAssemblySessions<String> sessions = new TrmsWeaponAssemblySessions<>(1_200, 4, 8);
        UUID player = UUID.randomUUID();
        var first = sessions.begin(player, 7, "part", 100).session();

        assertNotNull(sessions.get(player));
        assertEquals(0, sessions.purgeExpired(1_299));
        assertEquals(1, sessions.purgeExpired(1_300));
        assertEquals(1_300, first.expiresAt());
        assertNull(sessions.get(player));
    }

    @Test
    void quitCleanupRemovesOnlyThePlayerSession() {
        TrmsWeaponAssemblySessions<String> sessions = new TrmsWeaponAssemblySessions<>(1_200, 4, 8);
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        var first = sessions.begin(firstPlayer, 7, "first", 100).session();
        var second = sessions.begin(secondPlayer, 8, "second", 100).session();

        assertTrue(sessions.remove(firstPlayer));
        assertNull(sessions.get(firstPlayer));
        assertEquals(second, sessions.get(secondPlayer));
        assertFalse(sessions.remove(firstPlayer, first.id()));
    }
}
