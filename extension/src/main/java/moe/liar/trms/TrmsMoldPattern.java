package moe.liar.trms;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Horizon/Minecraft serialization adapter for the shared immutable mold value.
 *
 * <p>The Extension remains authoritative over when this value can replace a
 * block entity's state. The common library owns the deterministic pattern
 * format, coordinate mapping, and eight-neighbour rule.</p>
 */
public final class TrmsMoldPattern {
    public static final int INTERIOR_MIN = moe.liar.trms.common.MoldPattern.INTERIOR_MIN;
    public static final int INTERIOR_MAX = moe.liar.trms.common.MoldPattern.INTERIOR_MAX;
    public static final int INTERIOR_WIDTH = moe.liar.trms.common.MoldPattern.INTERIOR_WIDTH;
    public static final int BIT_COUNT = moe.liar.trms.common.MoldPattern.BIT_COUNT;
    public static final int BYTE_COUNT = moe.liar.trms.common.MoldPattern.BYTE_COUNT;
    public static final int FORMAT_VERSION = moe.liar.trms.common.MoldPattern.FORMAT_VERSION;

    /** Persistent component form with an explicit envelope version. */
    public static final Codec<TrmsMoldPattern> CODEC = Codec.pair(
            Codec.INT.fieldOf("format").codec(),
            Codec.BYTE.listOf().fieldOf("bits").codec()
    ).comapFlatMap(
            serialized -> decodeSerialized(serialized.getFirst(), serialized.getSecond()),
            pattern -> Pair.of(FORMAT_VERSION, pattern.encodeByteList())
    );

    /** Fixed-width network form shared with the NeoForge client mod. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TrmsMoldPattern> STREAM_CODEC = StreamCodec.of(
            (buffer, pattern) -> buffer.writeBytes(pattern.toBytes()),
            buffer -> {
                byte[] bytes = new byte[BYTE_COUNT];
                buffer.readBytes(bytes);
                return fromBytes(bytes);
            }
    );

    private static final TrmsMoldPattern EMPTY = new TrmsMoldPattern(moe.liar.trms.common.MoldPattern.EMPTY);

    private final moe.liar.trms.common.MoldPattern value;

    private TrmsMoldPattern(moe.liar.trms.common.MoldPattern value) {
        this.value = value;
    }

    public static TrmsMoldPattern empty() {
        return EMPTY;
    }

    public static boolean isCarvableCell(int x, int z) {
        return moe.liar.trms.common.MoldPattern.isCarvableCell(x, z);
    }

    public static int carvingIndex(int x, int z) {
        return moe.liar.trms.common.MoldPattern.carvingIndex(x, z);
    }

    public boolean isCarved(int x, int z) {
        return value.isCarved(x, z);
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public boolean canCarve(int x, int z) {
        return value.canCarve(x, z);
    }

    public TrmsMoldPattern carve(int x, int z) {
        return new TrmsMoldPattern(value.carve(x, z));
    }

    public byte[] toBytes() {
        return value.toBytes();
    }

    public static TrmsMoldPattern fromBytes(byte[] bytes) {
        return new TrmsMoldPattern(moe.liar.trms.common.MoldPattern.fromBytes(bytes));
    }

    private static DataResult<TrmsMoldPattern> decodeSerialized(int formatVersion, List<Byte> encoded) {
        if (formatVersion != FORMAT_VERSION) {
            return DataResult.error(() -> "Unsupported TRMS mold pattern format " + formatVersion
                    + "; this server supports " + FORMAT_VERSION);
        }
        if (encoded.size() != BYTE_COUNT) {
            return DataResult.error(() -> "Mold pattern must contain exactly " + BYTE_COUNT + " bytes, got " + encoded.size());
        }
        byte[] bytes = new byte[BYTE_COUNT];
        for (int index = 0; index < BYTE_COUNT; index++) {
            bytes[index] = encoded.get(index);
        }
        try {
            return DataResult.success(fromBytes(bytes));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private List<Byte> encodeByteList() {
        byte[] bytes = toBytes();
        List<Byte> encoded = new ArrayList<>(BYTE_COUNT);
        for (byte value : bytes) {
            encoded.add(value);
        }
        return List.copyOf(encoded);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TrmsMoldPattern pattern && value.equals(pattern.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "TrmsMoldPattern[carvedCells=" + value.carvedCount() + "/" + BIT_COUNT + "]";
    }
}
