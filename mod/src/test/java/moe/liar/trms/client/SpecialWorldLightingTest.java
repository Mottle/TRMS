package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpecialWorldLightingTest {
    @Test
    void enablesAmbientOcclusionOnlyForNonEmittingAmbientMaterials() {
        assertTrue(SpecialWorldLighting.shouldUseAmbientOcclusion(0, true));
        assertFalse(SpecialWorldLighting.shouldUseAmbientOcclusion(1, true));
        assertFalse(SpecialWorldLighting.shouldUseAmbientOcclusion(0, false));
    }
}
