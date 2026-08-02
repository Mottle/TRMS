package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class MoldMoltenEffectsTest {
    @Test
    void particleOriginsStayInsideTheOnlyCarvedCellForEveryFacing() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(4, 6).orElseThrow();

        assertSurfaceInCell(pattern, Direction.SOUTH, 4, 6);
        assertSurfaceInCell(pattern, Direction.WEST, 9, 4);
        assertSurfaceInCell(pattern, Direction.NORTH, 11, 9);
        assertSurfaceInCell(pattern, Direction.EAST, 6, 11);
    }

    @Test
    void coolingLinearlyReducesButNeverEliminatesLocalMoltenEffectChances() {
        assertEquals(50, MoldMoltenEffects.intervalForCooling(50, 0));
        assertEquals(143, MoldMoltenEffects.intervalForCooling(50, 9));
        assertEquals(286, MoldMoltenEffects.intervalForCooling(100, 9));
    }

    private static void assertSurfaceInCell(MoldPattern pattern, Direction facing, int minX, int minZ) {
        MoldMoltenEffects.Surface surface = MoldMoltenEffects.randomSurface(pattern, facing,
                RandomSource.create(4815162342L));
        assertTrue(surface.x() >= minX / 16.0D && surface.x() < (minX + 1) / 16.0D,
                () -> "x escaped " + facing + " filled cell: " + surface);
        assertTrue(surface.z() >= minZ / 16.0D && surface.z() < (minZ + 1) / 16.0D,
                () -> "z escaped " + facing + " filled cell: " + surface);
    }
}
