package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoldCoolingTest {
    @Test
    void exposesTheAgreedThirtySecondTickBasedCoolingSchedule() {
        assertEquals(20, MoldCooling.TICK_INTERVAL);
        assertEquals(600, MoldCooling.TOTAL_TICKS);
        assertEquals(300, MoldCooling.DIMMING_START_TICKS);
        assertEquals(10, MoldCooling.VISUAL_STAGE_COUNT);
        assertEquals(1, MoldPersistence.FORMAT_VERSION);
        assertEquals("CoolingTicks", MoldPersistence.COOLING_TICKS_KEY);
    }

    @Test
    void keepsTheFirstFifteenSecondsFullyMoltenThenDimsAcrossVisualStages() {
        assertEquals(15, MoldCooling.lightLevel(0));
        assertEquals(1, MoldCooling.lightLevel(9));
        assertEquals(1.0F, MoldCooling.brightness(0), 0.0001F);
        assertEquals(0.35F, MoldCooling.brightness(9), 0.0001F);
        assertEquals(0, MoldCooling.visualStage(0));
        assertEquals(0, MoldCooling.visualStage(280));
        assertEquals(1, MoldCooling.visualStage(300));
        assertEquals(9, MoldCooling.visualStage(580));
        assertEquals(15, MoldCooling.lightLevelForElapsedTicks(280));
        assertEquals(14, MoldCooling.lightLevelForElapsedTicks(300));
        assertEquals(1, MoldCooling.lightLevelForElapsedTicks(580));
        assertEquals(1.0F, MoldCooling.brightnessForElapsedTicks(280), 0.0001F);
        assertTrue(MoldCooling.brightnessForElapsedTicks(300) < 1.0F);
        assertTrue(MoldCooling.isValidElapsedTicks(0));
        assertTrue(MoldCooling.isValidElapsedTicks(580));
        assertFalse(MoldCooling.isValidElapsedTicks(-20));
        assertFalse(MoldCooling.isValidElapsedTicks(10));
        assertFalse(MoldCooling.isValidElapsedTicks(600));
        assertThrows(IllegalArgumentException.class, () -> MoldCooling.lightLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> MoldCooling.brightness(10));
        assertThrows(IllegalArgumentException.class, () -> MoldCooling.visualStage(10));
    }

    @Test
    void eachSuccessiveStageOnlyDimsTheVisibleMoltenPresentation() {
        int previousLight = MoldCooling.INITIAL_LIGHT_LEVEL;
        float previousBrightness = 1.0F;
        for (int stage = 0; stage < MoldCooling.VISUAL_STAGE_COUNT; stage++) {
            int light = MoldCooling.lightLevel(stage);
            float brightness = MoldCooling.brightness(stage);
            assertTrue(light <= previousLight);
            assertTrue(light >= MoldCooling.FINAL_LIGHT_LEVEL);
            assertTrue(brightness <= previousBrightness);
            assertTrue(brightness >= MoldCooling.FINAL_BRIGHTNESS);
            previousLight = light;
            previousBrightness = brightness;
        }
    }

    @Test
    void everyTwentyTickCheckpointKeepsTheVisualCurveMonotonic() {
        int previousStage = 0;
        for (int elapsedTicks = 0; elapsedTicks < MoldCooling.TOTAL_TICKS;
             elapsedTicks += MoldCooling.TICK_INTERVAL) {
            int stage = MoldCooling.visualStage(elapsedTicks);
            assertTrue(stage >= previousStage);
            if (elapsedTicks < MoldCooling.DIMMING_START_TICKS) {
                assertEquals(0, stage);
            } else {
                assertTrue(stage >= 1);
            }
            previousStage = stage;
        }
    }

    @Test
    void allocatesEveryDerivedVisualStageAcrossTheDimmingWindow() {
        boolean[] observedStages = new boolean[MoldCooling.VISUAL_STAGE_COUNT];
        for (int elapsedTicks = 0; elapsedTicks < MoldCooling.TOTAL_TICKS;
             elapsedTicks += MoldCooling.TICK_INTERVAL) {
            observedStages[MoldCooling.visualStage(elapsedTicks)] = true;
        }

        for (int stage = 0; stage < observedStages.length; stage++) {
            assertTrue(observedStages[stage], "visual stage was never emitted: " + stage);
        }
    }

    @Test
    void recordsTwentyTicksPerUpdateAndCompletesOnlyOnTheThirtiethUpdate() {
        int elapsedTicks = 0;
        for (int update = 1; update < 30; update++) {
            assertFalse(MoldCooling.completesOnNextUpdate(elapsedTicks));
            elapsedTicks = MoldCooling.advanceElapsedTicks(elapsedTicks);
            assertEquals(update * MoldCooling.TICK_INTERVAL, elapsedTicks);
        }

        assertEquals(580, elapsedTicks);
        assertTrue(MoldCooling.completesOnNextUpdate(elapsedTicks));
        int finalElapsedTicks = elapsedTicks;
        assertThrows(IllegalStateException.class, () -> MoldCooling.advanceElapsedTicks(finalElapsedTicks));
    }
}
