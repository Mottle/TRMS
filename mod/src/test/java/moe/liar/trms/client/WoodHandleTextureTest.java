package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WoodHandleTextureTest {
    @Test
    void usesAnOpaqueSixteenPixelVanillaStickInspiredPalette() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(
                "/assets/trms/textures/block/wood_handle.png")) {
            BufferedImage image = ImageIO.read(resource);
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    assertEquals(0xFF, (argb >>> 24) & 0xFF, "handle texture must not contain transparent pixels");
                    colors.add(argb & 0x00FFFFFF);
                }
            }
            assertTrue(colors.size() >= 3, "pixel texture must contain visible wood colour variation");
        }
    }
}
