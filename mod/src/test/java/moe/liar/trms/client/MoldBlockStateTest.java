package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class MoldBlockStateTest {
    @Test
    void predictsTheSamePlacementFacingAsTheExtension() {
        assertEquals(Direction.SOUTH, MoldOrientation.forPlacement(Direction.NORTH));
        assertEquals(Direction.WEST, MoldOrientation.forPlacement(Direction.EAST));
        assertEquals(Direction.NORTH, MoldOrientation.forPlacement(Direction.SOUTH));
        assertEquals(Direction.EAST, MoldOrientation.forPlacement(Direction.WEST));
    }

    @Test
    void structureRotationsAndMirrorsTransformTheFacing() {
        List<Direction> horizontal = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

        for (Direction facing : horizontal) {
            for (Rotation rotation : Rotation.values()) {
                assertEquals(rotation.rotate(facing), MoldOrientation.rotate(facing, rotation));
            }
            for (Mirror mirror : Mirror.values()) {
                assertEquals(mirror.getRotation(facing).rotate(facing), MoldOrientation.mirror(facing, mirror));
            }
        }
    }
}
