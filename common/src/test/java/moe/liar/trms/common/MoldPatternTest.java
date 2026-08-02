package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for the side-neutral implementation shared by both runtime artifacts. */
class MoldPatternTest {
    @Test
    void usesTheStableRowMajorInteriorMapping() {
        assertEquals(0, MoldPattern.carvingIndex(1, 1));
        assertEquals(13, MoldPattern.carvingIndex(14, 1));
        assertEquals(182, MoldPattern.carvingIndex(1, 14));
        assertEquals(195, MoldPattern.carvingIndex(14, 14));
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.carvingIndex(0, 1));
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.carvingIndex(15, 14));
    }

    @Test
    void encodesExactlyTwentyFiveCanonicalBytes() {
        byte[] bytes = new byte[MoldPattern.BYTE_COUNT];
        bytes[0] = (byte) 0x81;
        bytes[24] = 0x08;

        MoldPattern pattern = MoldPattern.fromBytes(bytes);
        assertArrayEquals(bytes, pattern.toBytes());
        assertTrue(pattern.isCarved(1, 1));
        assertTrue(pattern.isCarved(8, 1));
        assertTrue(pattern.isCarved(14, 14));
        assertFalse(pattern.isCarved(0, 1));

        byte[] invalid = new byte[MoldPattern.BYTE_COUNT];
        invalid[24] = 0x10;
        assertThrows(IllegalArgumentException.class, () -> MoldPattern.fromBytes(invalid));
    }

    @Test
    void permitsTheFirstCellThenOnlyEightNeighbourConnectedCells() {
        MoldPattern empty = MoldPattern.empty();
        assertTrue(empty.canCarve(14, 14));
        assertFalse(empty.canCarve(0, 1));

        MoldPattern first = empty.carve(8, 8);
        for (int deltaZ = -1; deltaZ <= 1; deltaZ++) {
            for (int deltaX = -1; deltaX <= 1; deltaX++) {
                if (deltaX != 0 || deltaZ != 0) {
                    assertTrue(first.canCarve(8 + deltaX, 8 + deltaZ));
                }
            }
        }
        assertFalse(first.canCarve(8, 8));
        assertFalse(first.canCarve(10, 8));
        assertFalse(first.canCarve(0, 8));
    }

    @Test
    void carvingReturnsANewImmutableValue() {
        MoldPattern before = MoldPattern.empty();
        MoldPattern after = before.carve(7, 7);

        assertTrue(before.isEmpty());
        assertFalse(before.isCarved(7, 7));
        assertTrue(after.isCarved(7, 7));
    }
}
