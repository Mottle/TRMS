package moe.liar.trms.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The fixed 16x16x2 collision volume of a placed ceramic mold. */
public final class MoldBlock extends Block implements EntityBlock {
    public static final VoxelShape FULL_THIN_SLAB = Block.box(0, 0, 0, 16, 2, 16);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MoldBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** Mirrors the Extension placement state so the client predicts the same orientation. */
    static Direction facingForPlacement(Direction playerFacing) {
        return MoldOrientation.forPlacement(playerFacing);
    }

    /** Reads the horizontal state defensively so legacy/default client states remain renderable. */
    static Direction facing(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, facingForPlacement(context.getHorizontalDirection()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, MoldOrientation.rotate(state.getValue(FACING), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, MoldOrientation.mirror(state.getValue(FACING), mirror));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_THIN_SLAB;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_THIN_SLAB;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasFullRigidSupport(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    /** Mirrors the Extension's placement rule for immediate client placement feedback. */
    private static boolean hasFullRigidSupport(BlockGetter level, BlockPos moldPos) {
        BlockPos supportPos = moldPos.below();
        BlockState support = level.getBlockState(supportPos);
        return !support.is(BlockTags.LEAVES)
                && support.getFluidState().isEmpty()
                && support.isCollisionShapeFullBlock(level, supportPos)
                && Block.canSupportRigidBlock(level, supportPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MoldBlockEntity(pos, state);
    }
}
