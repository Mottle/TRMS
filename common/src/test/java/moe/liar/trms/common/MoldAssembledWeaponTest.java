package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MoldAssembledWeaponTest {
    @Test
    void storesMaterialAndChosenConnectionPoint() {
        MoldPattern pattern = MoldPattern.EMPTY.carve(6, 6);
        MoldAssembledWeapon weapon = new MoldAssembledWeapon(pattern, MoldFillMaterial.GOLD,
                MoldWeaponAssembly.STICK_MATERIAL, new MoldWeaponAssembly.ConnectionPoint(6, 7));

        assertEquals(MoldFillMaterial.GOLD, weapon.material());
        assertEquals(6, weapon.connectionPoint().x());
        assertEquals(7, weapon.connectionPoint().z());
    }

    @Test
    void rejectsAConnectionThatIsNotBelowTheOutline() {
        MoldPattern pattern = MoldPattern.EMPTY.carve(6, 6);

        assertThrows(IllegalArgumentException.class, () -> new MoldAssembledWeapon(pattern,
                MoldFillMaterial.IRON, MoldWeaponAssembly.STICK_MATERIAL,
                new MoldWeaponAssembly.ConnectionPoint(6, 6)));
    }
}
