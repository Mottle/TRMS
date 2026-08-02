package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import moe.liar.trms.common.MoldCooling;
import org.junit.jupiter.api.Test;

/** Verifies that cooling persistence does not create client traffic before it changes presentation. */
class TrmsMoldCoolingSynchronizationTest {
    @Test
    void firstFifteenSecondsPersistWithoutSendingAClientStateUpdate() {
        for (int elapsedTicks = 0;
             elapsedTicks < MoldCooling.DIMMING_START_TICKS - MoldCooling.TICK_INTERVAL;
             elapsedTicks += MoldCooling.TICK_INTERVAL) {
            assertFalse(TrmsMoldBlockEntity.requiresCoolingVisualSynchronization(elapsedTicks),
                    "unexpected client update before visual dimming at " + elapsedTicks + " ticks");
        }

        assertTrue(TrmsMoldBlockEntity.requiresCoolingVisualSynchronization(
                MoldCooling.DIMMING_START_TICKS - MoldCooling.TICK_INTERVAL),
                "the fifteenth-second checkpoint begins visual dimming and must synchronize");
    }

    @Test
    void clientSynchronizationExactlyMatchesNonFinalVisualStageTransitions() {
        for (int elapsedTicks = 0;
             elapsedTicks < MoldCooling.TOTAL_TICKS - MoldCooling.TICK_INTERVAL;
             elapsedTicks += MoldCooling.TICK_INTERVAL) {
            int nextElapsedTicks = MoldCooling.advanceElapsedTicks(elapsedTicks);
            boolean expected = MoldCooling.visualStage(elapsedTicks)
                    != MoldCooling.visualStage(nextElapsedTicks);
            assertEquals(expected, TrmsMoldBlockEntity.requiresCoolingVisualSynchronization(elapsedTicks),
                    "unexpected client synchronization decision at " + elapsedTicks + " ticks");
        }

        assertFalse(TrmsMoldBlockEntity.requiresCoolingVisualSynchronization(
                MoldCooling.TOTAL_TICKS - MoldCooling.TICK_INTERVAL),
                "the final checkpoint synchronizes the fill-to-empty state rather than a filled cooling stage");
    }
}
