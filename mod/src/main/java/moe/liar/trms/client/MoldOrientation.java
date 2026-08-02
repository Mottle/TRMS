package moe.liar.trms.client;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/** Pure horizontal-orientation rules shared by the client mold block and its tests. */
final class MoldOrientation {
    private MoldOrientation() {
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
