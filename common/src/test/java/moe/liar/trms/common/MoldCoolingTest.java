package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoldCoolingTest {
    @Test
    void exposesTheAgreedTwentyTickTenStageCoolingSchedule() {
        assertEquals(20, MoldCooling.TICK_INTERVAL);
        assertEquals(10, MoldCooling.STAGE_COUNT);
        assertEquals(200, MoldCooling.TOTAL_TICKS);
        assertEquals("CoolingStage", MoldPersistence.COOLING_STAGE_KEY);
    }

    @Test
    void linearlyDimsLightAndTintAcrossOnlyVisibleStages() {
        assertEquals(15, MoldCooling.lightLevel(0));
        assertEquals(1, MoldCooling.lightLevel(9));
        assertEquals(1.0F, MoldCooling.brightness(0), 0.0001F);
        assertEquals(0.35F, MoldCooling.brightness(9), 0.0001F);
        assertTrue(MoldCooling.isValidStage(0));
        assertTrue(MoldCooling.isValidStage(9));
        assertFalse(MoldCooling.isValidStage(-1));
        assertFalse(MoldCooling.isValidStage(10));
        assertThrows(IllegalArgumentException.class, () -> MoldCooling.lightLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> MoldCooling.brightness(10));
    }

    @Test
    void eachSuccessiveStageOnlyDimsTheVisibleMoltenPresentation() {
        int previousLight = MoldCooling.INITIAL_LIGHT_LEVEL;
        float previousBrightness = 1.0F;
        for (int stage = 0; stage < MoldCooling.STAGE_COUNT; stage++) {
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
    void completesOnlyOnTheTenthScheduledCoolingUpdate() {
        int stage = 0;
        for (int update = 1; update < MoldCooling.STAGE_COUNT; update++) {
            assertFalse(MoldCooling.completesOnNextUpdate(stage));
            stage = MoldCooling.advanceStage(stage);
        }

        assertEquals(9, stage);
        assertTrue(MoldCooling.completesOnNextUpdate(stage));
        int finalStage = stage;
        assertThrows(IllegalStateException.class, () -> MoldCooling.advanceStage(finalStage));
    }
}
