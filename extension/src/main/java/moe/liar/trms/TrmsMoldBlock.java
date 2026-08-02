package moe.liar.trms;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The fixed two-pixel ceramic shell. Carving arrives through the dedicated C2S payload. */
final class TrmsMoldBlock extends Block implements EntityBlock {
    private static final MapCodec<TrmsMoldBlock> CODEC = simpleCodec(properties ->
            new TrmsMoldBlock(properties, null, TrmsContent::moldBlockEntityType));
    private static final VoxelShape FIXED_SLAB_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Supplier<BlockEntityType<TrmsMoldBlockEntity>> typeSupplier;

    TrmsMoldBlock(BlockBehaviour.Properties properties, @Nullable ResourceKey<Block> key,
                  Supplier<BlockEntityType<TrmsMoldBlockEntity>> typeSupplier) {
        super(key == null ? properties : properties.setId(key));
        this.typeSupplier = typeSupplier;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** New molds face their placer, as with other horizontally directional blocks. */
    static Direction facingForPlacement(Direction playerFacing) {
        return TrmsMoldOrientation.forPlacement(playerFacing);
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
        return state.setValue(FACING, TrmsMoldOrientation.rotate(state.getValue(FACING), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, TrmsMoldOrientation.mirror(state.getValue(FACING), mirror));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return typeSupplier.get().create(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FIXED_SLAB_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FIXED_SLAB_SHAPE;
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

    /**
     * Requires a dry, full-cube support whose upper face can rigidly support a block.
     * Leaves are intentionally excluded even if a custom leaf state reports a full shape.
     */
    static boolean hasFullRigidSupport(BlockGetter level, BlockPos moldPos) {
        BlockPos supportPos = moldPos.below();
        BlockState support = level.getBlockState(supportPos);
        return !support.is(BlockTags.LEAVES)
                && support.getFluidState().isEmpty()
                && support.isCollisionShapeFullBlock(level, supportPos)
                && Block.canSupportRigidBlock(level, supportPos);
    }

    /** Carving is an enabled forging operation only while the mold sits on a lodestone. */
    static boolean hasLodestoneCarvingBase(BlockGetter level, BlockPos moldPos) {
        return level.getBlockState(moldPos.below()).is(Blocks.LODESTONE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TrmsMoldBlockEntity mold) {
            TrmsMoldData.readItemPattern(stack, TrmsContent.moldPatternComponent())
                    .ifPresent(mold::restoreFromItemPattern);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack clone = super.getCloneItemStack(level, pos, state, includeData);
        if (level.getBlockEntity(pos) instanceof TrmsMoldBlockEntity mold) {
            TrmsMoldData.storeItemPattern(clone, TrmsContent.moldPatternComponent(), mold.pattern());
        }
        return clone;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof TrmsMoldBlockEntity mold) {
            for (ItemStack drop : drops) {
                if (drop.is(TrmsContent.moldItem())) {
                    TrmsMoldData.storeItemPattern(drop, TrmsContent.moldPatternComponent(), mold.pattern());
                }
            }
        }
        return drops;
    }
}
