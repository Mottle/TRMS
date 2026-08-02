package moe.liar.trms.client;

import java.util.Objects;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Client mirror of the server-owned mold state, including its immutable pattern and revision. */
public final class MoldBlockEntity extends BlockEntity {
    private MoldPattern pattern = MoldPattern.EMPTY;
    private long revision;
    private @Nullable MoldFillMaterial fillMaterial;
    /*
     * Render states are recreated every frame in Minecraft 26.1. Keep the
     * immutable derived data with this client BE instead; the BE's chunk/world
     * lifetime bounds the cache without a renderer-global Level reference.
     */
    private final MoldRenderCache renderCache = new MoldRenderCache();

    public MoldBlockEntity(BlockPos pos, BlockState state) {
        super(TrmsClientMod.MOLD_BLOCK_ENTITY.get(), pos, state);
    }

    public MoldPattern pattern() {
        return pattern;
    }

    public long revision() {
        return revision;
    }

    /** Returns the server-authoritative fill identity, or {@code null} when unfilled. */
    public @Nullable MoldFillMaterial fillMaterial() {
        return fillMaterial;
    }

    /** Filled molds are no longer a carving target, including unsupported future materials. */
    public boolean isFilled() {
        return fillMaterial != null;
    }

    MoldRenderCache renderCache() {
        return renderCache;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int format = input.getIntOr(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION);
        if (format != MoldPersistence.FORMAT_VERSION) {
            throw new IllegalStateException("Unsupported TRMS mold format: " + format);
        }
        pattern = input.read(MoldPersistence.PATTERN_KEY, MoldPattern.CODEC).orElseThrow(
                () -> new IllegalStateException("Missing or invalid TRMS mold Pattern envelope")
        );
        revision = input.getLongOr(MoldPersistence.REVISION_KEY, 0L);
        if (revision < 0L) {
            throw new IllegalStateException("TRMS mold revision must not be negative");
        }
        fillMaterial = readFillMaterial(input);
        validateFillState(pattern, fillMaterial);
        renderCache.invalidate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION);
        output.store(MoldPersistence.PATTERN_KEY, MoldPattern.CODEC, pattern);
        output.putLong(MoldPersistence.REVISION_KEY, revision);
        writeFillMaterial(output, fillMaterial);
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter getter) {
        super.applyImplicitComponents(getter);
        pattern = getter.getOrDefault(TrmsClientMod.MOLD_PATTERN.get(), MoldPattern.EMPTY);
        // Fill state belongs only to the placed block entity and is stripped from
        // the item component on every placement.
        fillMaterial = null;
        renderCache.invalidate();
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(TrmsClientMod.MOLD_PATTERN.get(), pattern);
    }

    static @Nullable MoldFillMaterial readFillMaterial(ValueInput input) {
        String serializedFill = input.getStringOr(MoldPersistence.FILL_MATERIAL_KEY, "");
        return serializedFill.isEmpty() ? null : MoldFillMaterial.of(serializedFill);
    }

    static void writeFillMaterial(ValueOutput output, @Nullable MoldFillMaterial material) {
        if (material != null) {
            output.putString(MoldPersistence.FILL_MATERIAL_KEY, material.id());
        }
    }

    /** Mirrors the Extension's persistence invariant before a client render state observes it. */
    static void validateFillState(MoldPattern pattern, @Nullable MoldFillMaterial material) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.carvedCount() == 0 && material != null) {
            throw new IllegalStateException("An empty TRMS mold cannot contain fill material");
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
}
