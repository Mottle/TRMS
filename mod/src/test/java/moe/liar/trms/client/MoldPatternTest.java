package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Compatibility tests for the NeoForge client's copy of the TRMS mold format. */
class MoldPatternTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mappingUsesTheAcceptedRowMajorInteriorContract() {
        assertEquals(0, MoldPattern.index(1, 1));
        assertEquals(13, MoldPattern.index(14, 1));
        assertEquals(182, MoldPattern.index(1, 14));
        assertEquals(195, MoldPattern.index(14, 14));
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.index(0, 1));
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.index(15, 14));
    }

    @Test
    void rawFormatIsTwentyFiveBytesAndDefensivelyImmutable() {
        byte[] encoded = new byte[MoldPattern.BYTE_COUNT];
        setBit(encoded, 0);
        setBit(encoded, 7);
        setBit(encoded, 8);
        setBit(encoded, 63);
        setBit(encoded, 64);
        setBit(encoded, 191);
        setBit(encoded, 195);

        MoldPattern pattern = MoldPattern.fromBytes(encoded);
        assertArrayEquals(encoded, pattern.toByteArray());
        encoded[0] = 0;
        assertTrue(pattern.isCarved(1, 1));
        assertTrue(pattern.isCarved(14, 14));
        assertTrue(pattern.isCarved(8, 5));
        assertTrue(pattern.isCarved(9, 5));
        assertTrue(pattern.isCarved(10, 14));
    }

    @Test
    void nonCanonicalAndWrongLengthRawFormatsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.fromBytes(new byte[24]));

        byte[] invalid = new byte[MoldPattern.BYTE_COUNT];
        invalid[MoldPattern.BYTE_COUNT - 1] = 0x10;
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.fromBytes(invalid));
    }

    @Test
    void carvingRequiresEightNeighbourContactAfterTheFirstHole() {
        assertTrue(MoldPattern.EMPTY.canCarveAt(14, 14));
        assertFalse(MoldPattern.EMPTY.canCarveAt(0, 1));

        MoldPattern first = MoldPattern.EMPTY.predictCarve(4, 4).orElseThrow();
        assertFalse(first.canCarveAt(4, 4));
        assertTrue(first.canCarveAt(3, 3));
        assertTrue(first.canCarveAt(4, 3));
        assertTrue(first.canCarveAt(5, 5));
        assertFalse(first.canCarveAt(6, 4));
        assertFalse(first.canCarveAt(14, 14));

        assertFalse(MoldPattern.EMPTY.isCarved(4, 4));
        assertTrue(first.isCarved(4, 4));
    }

    @Test
    void persistentEnvelopeAndRawNetworkCodecRoundTripWithoutAByteCountPrefix() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(2, 2).orElseThrow();
        JsonElement persistent = MoldPattern.CODEC.encodeStart(JsonOps.INSTANCE, pattern)
                .getOrThrow(error -> new AssertionError(error));

        assertEquals(1, persistent.getAsJsonObject().get("format").getAsInt());
        assertEquals(MoldPattern.BYTE_COUNT, persistent.getAsJsonObject().getAsJsonArray("bits").size());
        assertEquals(pattern, MoldPattern.CODEC.parse(JsonOps.INSTANCE, persistent)
                .getOrThrow(error -> new AssertionError(error)));

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        MoldPattern.STREAM_CODEC.encode(buffer, pattern);
        assertEquals(MoldPattern.BYTE_COUNT, buffer.readableBytes());
        assertEquals(pattern, MoldPattern.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    private static void setBit(byte[] bytes, int bitIndex) {
        bytes[bitIndex >>> 3] |= (byte) (1 << (bitIndex & 7));
    }
}
