package moe.liar.trms.client;

import java.util.Objects;
import moe.liar.trms.common.MoldCooling;
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
    private int coolingTicks;
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

    /** Returns whether this shared client mirror belongs to the clay blank block. */
    public boolean isBlank() {
        return getBlockState().is(TrmsClientMod.MOLD_BLANK.get());
    }

    /** Returns the server-authoritative elapsed cooling time while this mold remains filled. */
    public int coolingTicks() {
        return coolingTicks;
    }

    MoldRenderCache renderCache() {
        return renderCache;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int format = input.getInt(MoldPersistence.FORMAT_KEY).orElseThrow(
                () -> new IllegalStateException("Missing TRMS mold format")
        );
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
        coolingTicks = readCoolingTicks(input, fillMaterial);
        validateFillState(pattern, fillMaterial, coolingTicks);
        if (isBlank() && (fillMaterial != null || coolingTicks != 0)) {
            throw new IllegalStateException("A mold blank cannot contain fill or cooling state");
        }
        renderCache.invalidate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION);
        output.store(MoldPersistence.PATTERN_KEY, MoldPattern.CODEC, pattern);
        output.putLong(MoldPersistence.REVISION_KEY, revision);
        writeFillMaterial(output, fillMaterial);
        output.putInt(MoldPersistence.COOLING_TICKS_KEY, coolingTicks);
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter getter) {
        super.applyImplicitComponents(getter);
        pattern = getter.getOrDefault(TrmsClientMod.MOLD_PATTERN.get(), MoldPattern.EMPTY);
        // Fill state belongs only to the placed block entity and is stripped from
        // the item component on every placement.
        fillMaterial = null;
        coolingTicks = 0;
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

    /** Reads the optional unfilled value and requires an explicit tick count for every filled mold. */
    static int readCoolingTicks(ValueInput input, @Nullable MoldFillMaterial material) {
        var savedCoolingTicks = input.getInt(MoldPersistence.COOLING_TICKS_KEY);
        if (material != null && savedCoolingTicks.isEmpty()) {
            throw new IllegalStateException("A filled TRMS mold must contain CoolingTicks");
        }
        return savedCoolingTicks.orElse(0);
    }

    /** Mirrors the Extension's persistence invariant before a client render state observes it. */
    static void validateFillState(MoldPattern pattern, @Nullable MoldFillMaterial material, int coolingTicks) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.carvedCount() == 0 && material != null) {
            throw new IllegalStateException("An empty TRMS mold cannot contain fill material");
        }
        if (material != null && !MoldCooling.isValidElapsedTicks(coolingTicks)) {
            throw new IllegalStateException("Invalid TRMS mold cooling ticks: " + coolingTicks);
        }
        if (material == null && coolingTicks != 0) {
            throw new IllegalStateException("An unfilled TRMS mold cannot retain cooling progress");
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
