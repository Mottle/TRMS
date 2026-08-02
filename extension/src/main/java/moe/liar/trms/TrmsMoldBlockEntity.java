package moe.liar.trms;

import java.util.Objects;
import java.util.Optional;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-authoritative persistent state for one placed ceramic mold. */
final class TrmsMoldBlockEntity extends BlockEntity {
    private TrmsMoldPattern pattern = TrmsMoldPattern.empty();
    private Optional<MoldFillMaterial> fillMaterial = Optional.empty();
    private long revision;

    TrmsMoldBlockEntity(BlockEntityType<TrmsMoldBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    TrmsMoldPattern pattern() {
        return pattern;
    }

    long revision() {
        return revision;
    }

    Optional<MoldFillMaterial> fillMaterial() {
        return fillMaterial;
    }

    boolean canFill() {
        return !pattern.isEmpty() && fillMaterial.isEmpty();
    }

    /** Applies one server-validated carving and emits the ordinary BE update packet. */
    boolean carve(int localX, int localZ) {
        if (fillMaterial.isPresent() || !pattern.canCarve(localX, localZ)) {
            return false;
        }
        pattern = pattern.carve(localX, localZ);
        revision++;
        setChanged();
        return true;
    }

    /** Applies one server-validated, irreversible visual fill to this placed mold. */
    boolean fill(MoldFillMaterial material) {
        Objects.requireNonNull(material, "material");
        if (!canFill()) {
            return false;
        }
        fillMaterial = Optional.of(material);
        revision++;

        if (level instanceof ServerLevel serverLevel) {
            synchronizeFilledBlockState(serverLevel);
        }
        setChanged();
        return true;
    }

    /** Restores the portable item state on placement without trusting arbitrary block NBT. */
    void restoreFromItemPattern(TrmsMoldPattern itemPattern) {
        if (pattern.equals(itemPattern) && fillMaterial.isEmpty() && revision == 0L) {
            return;
        }
        pattern = itemPattern;
        fillMaterial = Optional.empty();
        revision = 0L;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        TrmsMoldData.State saved = TrmsMoldData.load(input);
        pattern = saved.pattern();
        revision = saved.revision();
        fillMaterial = saved.fillMaterial();
        if (level instanceof ServerLevel serverLevel) {
            synchronizeFilledBlockState(serverLevel);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        TrmsMoldData.save(output, pattern, revision, fillMaterial);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        TrmsMoldPattern componentPattern = components.get(TrmsContent.moldPatternComponent());
        if (componentPattern != null) {
            pattern = componentPattern;
            fillMaterial = Optional.empty();
            revision = 0L;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(TrmsContent.moldPatternComponent(), pattern);
    }

    /** Keeps the light-emission blockstate as a derived projection of BE material data. */
    private void synchronizeFilledBlockState(ServerLevel serverLevel) {
        BlockState state = getBlockState();
        if (!state.is(TrmsContent.MOLD.block())) {
            return;
        }
        boolean shouldBeFilled = fillMaterial.isPresent();
        if (state.getValue(TrmsMoldBlock.FILLED) != shouldBeFilled) {
            serverLevel.setBlock(worldPosition,
                    state.setValue(TrmsMoldBlock.FILLED, shouldBeFilled), Block.UPDATE_ALL);
            serverLevel.getLightEngine().checkBlock(worldPosition);
        }
    }
}
