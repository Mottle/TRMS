package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MoldWeaponPartTest {
    @Test
    void preservesThePatternAndMaterialOfOneCompletedCasting() {
        MoldPattern pattern = MoldPattern.empty().carve(7, 7).carve(8, 7);

        MoldWeaponPart part = new MoldWeaponPart(pattern, MoldFillMaterial.COPPER);

        assertEquals(pattern, part.pattern());
        assertEquals(MoldFillMaterial.COPPER, part.material());
    }

    @Test
    void rejectsAnEmptyOrIncompleteCastingDefinition() {
        assertThrows(IllegalArgumentException.class,
                () -> new MoldWeaponPart(MoldPattern.empty(), MoldFillMaterial.IRON));
        assertThrows(NullPointerException.class,
                () -> new MoldWeaponPart(null, MoldFillMaterial.IRON));
        assertThrows(NullPointerException.class,
                () -> new MoldWeaponPart(MoldPattern.empty().carve(7, 7), null));
    }
}
