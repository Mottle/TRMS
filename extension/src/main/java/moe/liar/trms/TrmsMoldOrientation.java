package moe.liar.trms;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** Pure horizontal-orientation rules shared by the Extension mold block and its tests. */
final class TrmsMoldOrientation {
    private TrmsMoldOrientation() {
    }

    static Direction forPlacement(Direction playerFacing) {
        return playerFacing.getOpposite();
    }

    static Direction rotate(Direction facing, Rotation rotation) {
        return rotation.rotate(facing);
    }

    static Direction mirror(Direction facing, Mirror mirror) {
        return rotate(facing, mirror.getRotation(facing));
    }
}
