package moe.liar.trms.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * NeoForge serialization and presentation adapter for the shared mold pattern.
 *
 * <p>All coordinate, bit-layout, and eight-neighbour logic lives in
 * {@code trms-common}. This client-only type owns only Minecraft codec
 * adaptation and the explicitly non-authoritative prediction convenience
 * needed by rendering tests.</p>
 */
public final class MoldPattern {
    public static final int INNER_SIZE = moe.liar.trms.common.MoldPattern.INTERIOR_WIDTH;
    public static final int BIT_COUNT = moe.liar.trms.common.MoldPattern.BIT_COUNT;
    public static final int BYTE_COUNT = moe.liar.trms.common.MoldPattern.BYTE_COUNT;
    public static final MoldPattern EMPTY = new MoldPattern(moe.liar.trms.common.MoldPattern.EMPTY);

    /** Persistent format: exactly {"format":1,"bits":[25 signed bytes]}. */
    public static final Codec<MoldPattern> CODEC = PatternEnvelope.CODEC.comapFlatMap(
            MoldPattern::fromEnvelope,
            MoldPattern::toEnvelope
    );

    /** The network format is exactly 25 raw bytes, with no length prefix. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MoldPattern> STREAM_CODEC = StreamCodec.of(
            (buffer, pattern) -> buffer.writeBytes(pattern.toByteArray()),
            buffer -> {
                byte[] values = new byte[BYTE_COUNT];
                buffer.readBytes(values);
                return fromBytes(values);
            }
    );

    private final moe.liar.trms.common.MoldPattern value;

    private MoldPattern(moe.liar.trms.common.MoldPattern value) {
        this.value = value;
    }

    public static MoldPattern fromBytes(byte[] bytes) {
        return new MoldPattern(moe.liar.trms.common.MoldPattern.fromBytes(bytes));
    }

    public byte[] toByteArray() {
        return value.toBytes();
    }

    public boolean isCarved(int x, int z) {
        return value.isCarved(x, z);
    }

    /** Returns whether a new hole is legal under the shared eight-neighbour rule. */
    public boolean canCarveAt(int x, int z) {
        return value.canCarve(x, z);
    }

    /**
     * Builds a presentation-only prediction for tests and client-side rule
     * evaluation. It never changes a world block entity; only the Extension
     * may accept a carve and synchronize its resulting pattern.
     */
    java.util.Optional<MoldPattern> predictCarve(int x, int z) {
        return value.tryCarve(x, z).map(MoldPattern::new);
    }

    public int carvedCount() {
        return value.carvedCount();
    }

    public static boolean isInnerCoordinate(int x, int z) {
        return moe.liar.trms.common.MoldPattern.isCarvableCell(x, z);
    }

    public static int index(int x, int z) {
        return moe.liar.trms.common.MoldPattern.carvingIndex(x, z);
    }

    private static DataResult<MoldPattern> fromEnvelope(PatternEnvelope envelope) {
        if (envelope.format() != moe.liar.trms.common.MoldPattern.FORMAT_VERSION) {
            return DataResult.error(() -> "Unsupported TRMS mold pattern format: " + envelope.format());
        }
        if (envelope.bits().size() != BYTE_COUNT) {
            return DataResult.error(() -> "A TRMS mold pattern must contain exactly 25 bytes");
        }
        byte[] bytes = new byte[BYTE_COUNT];
        for (int index = 0; index < BYTE_COUNT; index++) {
            bytes[index] = envelope.bits().get(index);
        }
        try {
            return DataResult.success(fromBytes(bytes));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private PatternEnvelope toEnvelope() {
        byte[] bytes = toByteArray();
        List<Byte> values = new ArrayList<>(BYTE_COUNT);
        for (byte value : bytes) {
            values.add(value);
        }
        return new PatternEnvelope(moe.liar.trms.common.MoldPattern.FORMAT_VERSION, List.copyOf(values));
    }

    private record PatternEnvelope(int format, List<Byte> bits) {
        private static final Codec<PatternEnvelope> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("format").forGetter(PatternEnvelope::format),
                Codec.BYTE.listOf().fieldOf("bits").forGetter(PatternEnvelope::bits)
        ).apply(instance, PatternEnvelope::new));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MoldPattern pattern && value.equals(pattern.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "MoldPattern[carved=" + carvedCount() + "/" + BIT_COUNT + "]";
    }
}
