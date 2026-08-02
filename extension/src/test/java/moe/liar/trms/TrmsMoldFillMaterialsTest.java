package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsMoldFillMaterialsTest {
    @BeforeAll
    static void initializeMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mapsOnlySupportedVanillaIngotsToExtensibleMaterialIds() {
        assertEquals(MoldFillMaterial.COPPER,
                TrmsMoldFillMaterials.forIngredient(Items.COPPER_INGOT)
                        .orElseThrow().material());
        assertEquals(MoldFillMaterial.IRON,
                TrmsMoldFillMaterials.forIngredient(Items.IRON_INGOT)
                        .orElseThrow().material());
        assertTrue(TrmsMoldFillMaterials.forIngredient(Items.GOLD_INGOT).isEmpty());
        assertTrue(TrmsMoldFillMaterials.forIngredient(Items.AIR).isEmpty());
    }

    @Test
    void moltenDemoMaterialsEmitVanillaLavaLight() {
        assertEquals(0, TrmsMoldFillMaterials.lightLevel(false, 0));
        assertEquals(15, TrmsMoldFillMaterials.lightLevel(true, 0));
        assertEquals(1, TrmsMoldFillMaterials.lightLevel(true, 9));
    }
}
