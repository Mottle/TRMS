package moe.liar.trms.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, side-neutral state of a ceramic mold's mutable upper interior.
 *
 * <p>The physical lower layer and outer rim are deliberately absent from this
 * value. This class owns only deterministic format and carving-rule semantics;
 * the Extension alone decides whether a requested carve is authorized and may
 * write the resulting value to a world block entity.</p>
 */
public final class MoldPattern {
    public static final int INTERIOR_MIN = 1;
    public static final int INTERIOR_MAX = 14;
    public static final int INTERIOR_WIDTH = 14;
    public static final int BIT_COUNT = INTERIOR_WIDTH * INTERIOR_WIDTH;
    public static final int BYTE_COUNT = (BIT_COUNT + Byte.SIZE - 1) / Byte.SIZE;
    public static final int FORMAT_VERSION = 1;

    private static final long LAST_WORD_MASK = 0xFL;
    public static final MoldPattern EMPTY = new MoldPattern(0L, 0L, 0L, 0L);

    private final long word0;
    private final long word1;
    private final long word2;
    private final long word3;

    private MoldPattern(long word0, long word1, long word2, long word3) {
        if ((word3 & ~LAST_WORD_MASK) != 0L) {
            throw new IllegalArgumentException("Mold pattern contains bits above " + BIT_COUNT);
        }
        this.word0 = word0;
        this.word1 = word1;
        this.word2 = word2;
        this.word3 = word3;
    }

    public static MoldPattern empty() {
        return EMPTY;
    }

    public static boolean isCarvableCell(int x, int z) {
        return x >= INTERIOR_MIN && x <= INTERIOR_MAX && z >= INTERIOR_MIN && z <= INTERIOR_MAX;
    }

    public static int carvingIndex(int x, int z) {
        if (!isCarvableCell(x, z)) {
            throw new IllegalArgumentException("Mold cell is not carvable: x=" + x + ", z=" + z);
        }
        return (z - INTERIOR_MIN) * INTERIOR_WIDTH + (x - INTERIOR_MIN);
    }

    public boolean isCarved(int x, int z) {
        return isCarvableCell(x, z) && isCarvedIndex(carvingIndex(x, z));
    }

    public boolean isEmpty() {
        return word0 == 0L && word1 == 0L && word2 == 0L && word3 == 0L;
    }

    /** Returns whether this immutable pattern accepts the requested next cell. */
    public boolean canCarve(int x, int z) {
        if (!isCarvableCell(x, z) || isCarved(x, z)) {
            return false;
        }
        if (isEmpty()) {
            return true;
        }
        for (int deltaZ = -1; deltaZ <= 1; deltaZ++) {
            for (int deltaX = -1; deltaX <= 1; deltaX++) {
                if (deltaX != 0 || deltaZ != 0) {
                    int neighborX = x + deltaX;
                    int neighborZ = z + deltaZ;
                    if (isCarved(neighborX, neighborZ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Returns a new pattern after one legal carve, or rejects an illegal request. */
    public MoldPattern carve(int x, int z) {
        if (!canCarve(x, z)) {
            throw new IllegalArgumentException("Mold cell is not a valid next carving: x=" + x + ", z=" + z);
        }
        int index = carvingIndex(x, z);
        long mask = 1L << (index & 63);
        return switch (index >>> 6) {
            case 0 -> new MoldPattern(word0 | mask, word1, word2, word3);
            case 1 -> new MoldPattern(word0, word1 | mask, word2, word3);
            case 2 -> new MoldPattern(word0, word1, word2 | mask, word3);
            case 3 -> new MoldPattern(word0, word1, word2, word3 | mask);
            default -> throw new IllegalStateException("Unexpected carving index: " + index);
        };
    }

    public Optional<MoldPattern> tryCarve(int x, int z) {
        return canCarve(x, z) ? Optional.of(carve(x, z)) : Optional.empty();
    }

    public int carvedCount() {
        return Long.bitCount(word0) + Long.bitCount(word1) + Long.bitCount(word2) + Long.bitCount(word3);
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[BYTE_COUNT];
        writeLittleEndianWord(bytes, 0, word0, Long.BYTES);
        writeLittleEndianWord(bytes, Long.BYTES, word1, Long.BYTES);
        writeLittleEndianWord(bytes, Long.BYTES * 2, word2, Long.BYTES);
        writeLittleEndianWord(bytes, Long.BYTES * 3, word3, 1);
        return bytes;
    }

    public static MoldPattern fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != BYTE_COUNT) {
            throw new IllegalArgumentException("Mold pattern must contain exactly " + BYTE_COUNT + " bytes, got " + bytes.length);
        }
        return new MoldPattern(
                readLittleEndianWord(bytes, 0, Long.BYTES),
                readLittleEndianWord(bytes, Long.BYTES, Long.BYTES),
                readLittleEndianWord(bytes, Long.BYTES * 2, Long.BYTES),
                readLittleEndianWord(bytes, Long.BYTES * 3, 1)
        );
    }

    private boolean isCarvedIndex(int index) {
        long word = switch (index >>> 6) {
            case 0 -> word0;
            case 1 -> word1;
            case 2 -> word2;
            case 3 -> word3;
            default -> throw new IllegalStateException("Unexpected carving index: " + index);
        };
        return (word & (1L << (index & 63))) != 0L;
    }

    private static long readLittleEndianWord(byte[] bytes, int offset, int byteCount) {
        long value = 0L;
        for (int index = 0; index < byteCount; index++) {
            value |= (long) (bytes[offset + index] & 0xFF) << (index * Byte.SIZE);
        }
        return value;
    }

    private static void writeLittleEndianWord(byte[] bytes, int offset, long value, int byteCount) {
        for (int index = 0; index < byteCount; index++) {
            bytes[offset + index] = (byte) (value >>> (index * Byte.SIZE));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MoldPattern pattern)) {
            return false;
        }
        return word0 == pattern.word0 && word1 == pattern.word1 && word2 == pattern.word2 && word3 == pattern.word3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(word0, word1, word2, word3);
    }

    @Override
    public String toString() {
        return "MoldPattern[carvedCells=" + carvedCount() + "/" + BIT_COUNT + "]";
    }
}
