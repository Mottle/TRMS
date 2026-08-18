package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

/** Minimal dependency-free PNG reader for validating the project's RGBA test assets. */
final class PngTestImage {
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final int width;
    private final int height;
    private final int[] argb;

    private PngTestImage(int width, int height, int[] argb) {
        this.width = width;
        this.height = height;
        this.argb = argb;
    }

    static PngTestImage read(InputStream input) throws IOException {
        try (DataInputStream png = new DataInputStream(input)) {
            byte[] signature = png.readNBytes(SIGNATURE.length);
            assertEquals(SIGNATURE.length, signature.length, "truncated PNG signature");
            for (int index = 0; index < SIGNATURE.length; index++) {
                assertEquals(SIGNATURE[index], signature[index], "invalid PNG signature");
            }

            int width = -1;
            int height = -1;
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            boolean ended = false;
            while (!ended) {
                int length = png.readInt();
                assertTrue(length >= 0, "negative PNG chunk length");
                byte[] type = png.readNBytes(4);
                assertEquals(4, type.length, "truncated PNG chunk type");
                byte[] data = png.readNBytes(length);
                assertEquals(length, data.length, "truncated PNG chunk data");
                png.readInt(); // The assets are local test fixtures; CRC is not part of the assertions.

                String chunk = new String(type, java.nio.charset.StandardCharsets.US_ASCII);
                switch (chunk) {
                    case "IHDR" -> {
                        try (DataInputStream header = new DataInputStream(new ByteArrayInputStream(data))) {
                            width = header.readInt();
                            height = header.readInt();
                            int bitDepth = header.readUnsignedByte();
                            int colorType = header.readUnsignedByte();
                            int compression = header.readUnsignedByte();
                            int filter = header.readUnsignedByte();
                            int interlace = header.readUnsignedByte();
                            assertEquals(8, bitDepth, "test PNG must use 8-bit samples");
                            assertEquals(6, colorType, "test PNG must use RGBA pixels");
                            assertEquals(0, compression, "unsupported PNG compression method");
                            assertEquals(0, filter, "unsupported PNG filter method");
                            assertEquals(0, interlace, "interlaced test PNGs are unsupported");
                        }
                    }
                    case "IDAT" -> compressed.write(data);
                    case "IEND" -> ended = true;
                    default -> {
                        // Ancillary chunks are intentionally ignored.
                    }
                }
            }
            assertTrue(width > 0 && height > 0, "PNG is missing a valid IHDR");
            byte[] scanlines;
            try (InputStream inflated = new InflaterInputStream(
                    new ByteArrayInputStream(compressed.toByteArray()))) {
                scanlines = inflated.readAllBytes();
            }
            return decodeScanlines(width, height, scanlines);
        }
    }

    private static PngTestImage decodeScanlines(int width, int height, byte[] scanlines) {
        int bytesPerPixel = 4;
        int rowBytes = width * bytesPerPixel;
        assertEquals(height * (rowBytes + 1), scanlines.length, "unexpected PNG scanline length");
        int[] argb = new int[width * height];
        byte[] previous = new byte[rowBytes];
        byte[] current = new byte[rowBytes];
        int offset = 0;
        for (int y = 0; y < height; y++) {
            int filter = Byte.toUnsignedInt(scanlines[offset++]);
            System.arraycopy(scanlines, offset, current, 0, rowBytes);
            offset += rowBytes;
            unfilter(current, previous, filter, bytesPerPixel);
            for (int x = 0; x < width; x++) {
                int pixel = x * bytesPerPixel;
                int red = Byte.toUnsignedInt(current[pixel]);
                int green = Byte.toUnsignedInt(current[pixel + 1]);
                int blue = Byte.toUnsignedInt(current[pixel + 2]);
                int alpha = Byte.toUnsignedInt(current[pixel + 3]);
                argb[y * width + x] = alpha << 24 | red << 16 | green << 8 | blue;
            }
            byte[] swap = previous;
            previous = current;
            current = swap;
        }
        return new PngTestImage(width, height, argb);
    }

    private static void unfilter(byte[] row, byte[] previous, int filter, int bytesPerPixel) {
        assertTrue(filter >= 0 && filter <= 4, "unsupported PNG row filter: " + filter);
        for (int index = 0; index < row.length; index++) {
            int left = index >= bytesPerPixel ? Byte.toUnsignedInt(row[index - bytesPerPixel]) : 0;
            int above = Byte.toUnsignedInt(previous[index]);
            int upperLeft = index >= bytesPerPixel
                    ? Byte.toUnsignedInt(previous[index - bytesPerPixel]) : 0;
            int value = Byte.toUnsignedInt(row[index]);
            int predictor = switch (filter) {
                case 0 -> 0;
                case 1 -> left;
                case 2 -> above;
                case 3 -> (left + above) / 2;
                case 4 -> paeth(left, above, upperLeft);
                default -> throw new AssertionError("validated above");
            };
            row[index] = (byte) (value + predictor);
        }
    }

    private static int paeth(int left, int above, int upperLeft) {
        int estimate = left + above - upperLeft;
        int leftDistance = Math.abs(estimate - left);
        int aboveDistance = Math.abs(estimate - above);
        int upperLeftDistance = Math.abs(estimate - upperLeft);
        if (leftDistance <= aboveDistance && leftDistance <= upperLeftDistance) {
            return left;
        }
        return aboveDistance <= upperLeftDistance ? above : upperLeft;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int pixel(int x, int y) {
        return argb[y * width + x];
    }
}
