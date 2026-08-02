package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TrmsMoldSupportTest {
    private static final BlockPos MOLD_POS = new BlockPos(0, 64, 0);

    @Test
    void acceptsAFullRigidDryBlock() {
        assertTrue(TrmsMoldBlock.hasFullRigidSupport(new SingleStateBlockGetter(Blocks.STONE.defaultBlockState()), MOLD_POS));
    }

    @Test
    void rejectsLeavesLiquidsAndPartialSupports() {
        assertFalse(TrmsMoldBlock.hasFullRigidSupport(new SingleStateBlockGetter(Blocks.OAK_LEAVES.defaultBlockState()), MOLD_POS));
        assertFalse(TrmsMoldBlock.hasFullRigidSupport(new SingleStateBlockGetter(Blocks.WATER.defaultBlockState()), MOLD_POS));
        assertFalse(TrmsMoldBlock.hasFullRigidSupport(new SingleStateBlockGetter(Blocks.STONE_SLAB.defaultBlockState()), MOLD_POS));
    }

    @Test
    void acceptsCarvingOnlyOnALodestoneBase() {
        assertTrue(TrmsMoldBlock.hasLodestoneCarvingBase(new SingleStateBlockGetter(Blocks.LODESTONE.defaultBlockState()), MOLD_POS));
        assertFalse(TrmsMoldBlock.hasLodestoneCarvingBase(new SingleStateBlockGetter(Blocks.STONE.defaultBlockState()), MOLD_POS));
    }

    @Test
    void facesThePlacerWhenPlacedFromEveryHorizontalDirection() {
        assertEquals(Direction.SOUTH, TrmsMoldOrientation.forPlacement(Direction.NORTH));
        assertEquals(Direction.WEST, TrmsMoldOrientation.forPlacement(Direction.EAST));
        assertEquals(Direction.NORTH, TrmsMoldOrientation.forPlacement(Direction.SOUTH));
        assertEquals(Direction.EAST, TrmsMoldOrientation.forPlacement(Direction.WEST));
    }

    @Test
    void structureRotationsAndMirrorsTransformTheFacing() {
        List<Direction> horizontal = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

        for (Direction facing : horizontal) {
            for (Rotation rotation : Rotation.values()) {
                assertEquals(rotation.rotate(facing), TrmsMoldOrientation.rotate(facing, rotation));
            }
            for (Mirror mirror : Mirror.values()) {
                assertEquals(mirror.getRotation(facing).rotate(facing),
                        TrmsMoldOrientation.mirror(facing, mirror));
            }
        }
    }

    private record SingleStateBlockGetter(BlockState state) implements BlockGetter {
        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public @Nullable BlockState getBlockStateIfLoaded(BlockPos pos) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return state.getFluidState();
        }

        @Override
        public @Nullable FluidState getFluidIfLoaded(BlockPos pos) {
            return state.getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
