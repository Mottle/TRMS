package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WoodHandleTextureTest {
    @Test
    void usesAnOpaqueSixteenPixelVanillaStickInspiredPalette() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(
                "/assets/trms/textures/block/wood_handle.png")) {
            PngTestImage image = PngTestImage.read(resource);
            assertEquals(16, image.width());
            assertEquals(16, image.height());
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.height(); y++) {
                for (int x = 0; x < image.width(); x++) {
                    int argb = image.pixel(x, y);
                    assertEquals(0xFF, (argb >>> 24) & 0xFF, "handle texture must not contain transparent pixels");
                    colors.add(argb & 0x00FFFFFF);
                }
            }
            assertTrue(colors.size() >= 3, "pixel texture must contain visible wood colour variation");
        }
    }
}
