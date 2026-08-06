package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeaponAssemblyLayoutTest {
    @Test
    void usesTheCalibratedMaximumOnLargeScreens() {
        WeaponAssemblyLayout layout = WeaponAssemblyLayout.forScreen(1920, 1080);

        assertEquals(12, layout.cellSize());
        assertEquals(168, layout.gridWidth());
        assertEquals(312, layout.gridHeight());
    }

    @Test
    void shrinksToAnIntegerThatKeepsTheWholePreviewVisible() {
        WeaponAssemblyLayout layout = WeaponAssemblyLayout.forScreen(480, 270);

        assertEquals(8, layout.cellSize());
        assertTrue(layout.originX() >= 0);
        assertTrue(layout.originY() >= 0);
        assertTrue(layout.originX() + layout.gridWidth() <= 480);
        assertTrue(layout.originY() + layout.gridHeight() <= 270);
    }

    @Test
    void keepsAOnePixelPreviewAvailableAtTheSmallestValidWindow() {
        WeaponAssemblyLayout layout = WeaponAssemblyLayout.forScreen(1, 1);

        assertEquals(1, layout.cellSize());
        assertEquals(14, layout.gridWidth());
        assertEquals(26, layout.gridHeight());
    }
}
