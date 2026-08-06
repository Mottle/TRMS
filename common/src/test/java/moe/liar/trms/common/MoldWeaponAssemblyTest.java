package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoldWeaponAssemblyTest {
    @Test
    void exposesOnlyEmptyPixelsDirectlyBelowOutline() {
        MoldPattern pattern = MoldPattern.EMPTY.carve(4, 4).carve(5, 4).carve(5, 5);

        assertEquals(java.util.List.of(
                new MoldWeaponAssembly.ConnectionPoint(4, 5),
                new MoldWeaponAssembly.ConnectionPoint(5, 6)),
                MoldWeaponAssembly.legalConnectionPoints(pattern));
        assertFalse(MoldWeaponAssembly.isLegalConnection(pattern, 5, 5));
        assertTrue(MoldWeaponAssembly.isLegalConnection(pattern, 4, 5));
    }

    @Test
    void bottomEdgeCanUseThePixelImmediatelyOutsideTheInterior() {
        MoldPattern pattern = MoldPattern.EMPTY.carve(7, MoldPattern.INTERIOR_MAX);

        assertEquals(java.util.List.of(new MoldWeaponAssembly.ConnectionPoint(7, 15)),
                MoldWeaponAssembly.legalConnectionPoints(pattern));
    }
}
