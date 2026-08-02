package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import moe.liar.trms.common.MoldCooling;
import moe.liar.trms.common.MoldFillMaterial;
import org.junit.jupiter.api.Test;

class MoldFillVisualTest {
    @Test
    void mapsCopperIronAndGoldToStableSolidMaterialTints() {
        assertEquals(MoldFillVisual.COPPER_COLOR,
                MoldFillVisual.forMaterial(MoldFillMaterial.COPPER).baseColor());
        assertEquals(MoldFillVisual.IRON_COLOR,
                MoldFillVisual.forMaterial(MoldFillMaterial.IRON).baseColor());
        assertEquals(MoldFillVisual.GOLD_COLOR,
                MoldFillVisual.forMaterial(MoldFillMaterial.GOLD).baseColor());
    }

    @Test
    void goldUsesABrighterTintOnlyWhileItRemainsMolten() {
        MoldFillVisual gold = MoldFillVisual.forMaterial(MoldFillMaterial.GOLD);

        assertEquals(MoldFillVisual.GOLD_COLOR, gold.baseColor());
        assertEquals(MoldFillVisual.MOLTEN_GOLD_COLOR, gold.colorForCoolingTicks(0));
        assertEquals(MoldFillVisual.MOLTEN_GOLD_COLOR, gold.colorForCoolingTicks(280));
        assertTrue(gold.colorForCoolingTicks(300) != MoldFillVisual.MOLTEN_GOLD_COLOR);
    }

    @Test
    void unknownMaterialsRemainVisibleThroughTheDiagnosticFallbackTint() {
        assertEquals(MoldFillVisual.UNKNOWN_COLOR,
                MoldFillVisual.forMaterial(MoldFillMaterial.of("example:future_alloy")).baseColor());
    }

    @Test
    void fillMeshesUseTintableMoltenAndSolidMaterialSprites() {
        assertNull(MoldFillVisual.forMaterial(null));
        assertEquals("trms:block/molten_still", MoldMeshBuilder.MOLTEN_STILL_SPRITE.texture().toString());
        assertEquals("trms:block/molten_flow", MoldMeshBuilder.MOLTEN_FLOW_SPRITE.texture().toString());
        assertEquals("trms:block/solid_metal", MoldMeshBuilder.SOLID_METAL_SPRITE.texture().toString());
    }

    @Test
    void coolingDarksTheMaterialTintWithoutMakingTheFillTransparent() {
        MoldFillVisual copper = MoldFillVisual.forMaterial(MoldFillMaterial.COPPER);

        assertEquals(MoldFillVisual.COPPER_COLOR, copper.colorForCoolingTicks(280));
        assertTrue(copper.colorForCoolingTicks(300) != MoldFillVisual.COPPER_COLOR);
        int cooled = copper.colorForCoolingTicks(580);

        assertEquals(0xFF, cooled >>> 24);
        assertEquals(Math.round((MoldFillVisual.COPPER_COLOR >>> 16 & 0xFF) * MoldCooling.FINAL_BRIGHTNESS),
                cooled >>> 16 & 0xFF);
    }

    @Test
    void worldAmbientOcclusionMultipliesTheMoltenMaterialTint() {
        assertEquals(0xFF603621,
                MoldMeshBuilder.applyWorldLightingTint(0xFF808080, MoldFillVisual.COPPER_COLOR));
        assertEquals(MoldFillVisual.COPPER_COLOR,
                MoldMeshBuilder.applyWorldLightingTint(0xFFFFFFFF, MoldFillVisual.COPPER_COLOR));
    }

    @Test
    void moltenAnimationBasesRemainGreyscaleSoMaterialTintsCanChangeTheirHue() throws IOException {
        assertGreyscale("/assets/trms/textures/block/molten_still.png");
        assertGreyscale("/assets/trms/textures/block/molten_flow.png");
    }

    @Test
    void cooledWeaponPartTextureIsOpaqueWhiteSoSideFacesRemainPurelyTinted() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/assets/trms/textures/block/solid_metal.png")) {
            assertNotNull(stream, "missing pure metal base texture");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "invalid pure metal base texture");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    assertEquals(0xFFFFFFFF, image.getRGB(x, y),
                            "non-white metal texture pixel at (" + x + "," + y + ")");
                }
            }
        }
    }

    private void assertGreyscale(String resourcePath) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(resource, "missing molten animation base: " + resourcePath);
            BufferedImage image = ImageIO.read(resource);
            assertNotNull(image, "invalid PNG resource: " + resourcePath);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    int red = pixel >>> 16 & 0xFF;
                    int green = pixel >>> 8 & 0xFF;
                    int blue = pixel & 0xFF;
                    assertEquals(red, green, "non-greyscale green channel at " + resourcePath + " (" + x + "," + y + ")");
                    assertEquals(red, blue, "non-greyscale blue channel at " + resourcePath + " (" + x + "," + y + ")");
                }
            }
        }
    }
}
