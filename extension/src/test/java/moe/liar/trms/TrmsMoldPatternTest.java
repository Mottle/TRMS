package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsMoldPatternTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mappingUsesTheAcceptedRowMajorInteriorContract() {
        assertEquals(0, TrmsMoldPattern.carvingIndex(1, 1));
        assertEquals(13, TrmsMoldPattern.carvingIndex(14, 1));
        assertEquals(182, TrmsMoldPattern.carvingIndex(1, 14));
        assertEquals(195, TrmsMoldPattern.carvingIndex(14, 14));
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldPattern.carvingIndex(0, 1));
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldPattern.carvingIndex(15, 14));
    }

    @Test
    void roundTripPreservesAllBoundaryBitsAndReturnsDefensiveBytes() {
        byte[] serialized = new byte[TrmsMoldPattern.BYTE_COUNT];
        setBit(serialized, 0);
        setBit(serialized, 7);
        setBit(serialized, 8);
        setBit(serialized, 63);
        setBit(serialized, 64);
        setBit(serialized, 191);
        setBit(serialized, 195);
        TrmsMoldPattern pattern = TrmsMoldPattern.fromBytes(serialized);
        TrmsMoldPattern decoded = TrmsMoldPattern.fromBytes(serialized);

        assertEquals(pattern, decoded);
        assertArrayEquals(serialized, decoded.toBytes());
        serialized[0] = 0;
        assertTrue(decoded.isCarved(1, 1));
        assertTrue(decoded.isCarved(14, 14));
        assertTrue(decoded.isCarved(8, 5));
        assertTrue(decoded.isCarved(9, 5));
        assertTrue(decoded.isCarved(10, 14));
    }

    @Test
    void encodedFormRejectsWrongLengthAndNonCanonicalHighBits() {
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldPattern.fromBytes(new byte[24]));
        byte[] invalid = new byte[TrmsMoldPattern.BYTE_COUNT];
        invalid[24] = 0x10;
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldPattern.fromBytes(invalid));
    }

    @Test
    void firstCarvingCanStartAnywhereButAllFollowingCarvingsNeedEightNeighbourContact() {
        TrmsMoldPattern empty = TrmsMoldPattern.empty();
        assertTrue(empty.canCarve(14, 14));
        assertFalse(empty.canCarve(0, 1));

        TrmsMoldPattern first = empty.carve(4, 4);
        assertFalse(first.canCarve(4, 4));
        assertTrue(first.canCarve(3, 3));
        assertTrue(first.canCarve(4, 3));
        assertTrue(first.canCarve(5, 5));
        assertFalse(first.canCarve(6, 4));
        assertFalse(first.canCarve(14, 14));
    }

    @Test
    void everyEightNeighbourDirectionIsAcceptedButTheFixedBorderAndDuplicatesAreNot() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(8, 8);
        for (int deltaZ = -1; deltaZ <= 1; deltaZ++) {
            for (int deltaX = -1; deltaX <= 1; deltaX++) {
                if (deltaX != 0 || deltaZ != 0) {
                    assertTrue(pattern.canCarve(8 + deltaX, 8 + deltaZ));
                }
            }
        }
        assertFalse(pattern.canCarve(8, 8));
        assertFalse(pattern.canCarve(10, 8));
        assertFalse(pattern.canCarve(0, 8));
        assertFalse(pattern.canCarve(15, 8));
        assertFalse(pattern.canCarve(8, 0));
        assertFalse(pattern.canCarve(8, 15));
    }

    @Test
    void carvingIsImmutable() {
        TrmsMoldPattern before = TrmsMoldPattern.empty();
        TrmsMoldPattern after = before.carve(7, 7);

        assertTrue(before.isEmpty());
        assertFalse(before.isCarved(7, 7));
        assertTrue(after.isCarved(7, 7));
    }

    @Test
    void persistentEnvelopeIsVersionedWhileNetworkEncodingRemainsExactlyTwentyFiveBytes() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(2, 2);
        JsonElement encoded = TrmsMoldPattern.CODEC.encodeStart(JsonOps.INSTANCE, pattern)
                .getOrThrow(error -> new AssertionError(error));
        assertEquals(TrmsMoldPattern.FORMAT_VERSION, encoded.getAsJsonObject().get("format").getAsInt());
        assertEquals(TrmsMoldPattern.BYTE_COUNT, encoded.getAsJsonObject().getAsJsonArray("bits").size());
        assertEquals(pattern, TrmsMoldPattern.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError(error)));

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        TrmsMoldPattern.STREAM_CODEC.encode(buffer, pattern);
        assertEquals(TrmsMoldPattern.BYTE_COUNT, buffer.readableBytes());
        assertEquals(pattern, TrmsMoldPattern.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    private static void setBit(byte[] bytes, int index) {
        bytes[index >>> 3] |= (byte) (1 << (index & 7));
    }
}
