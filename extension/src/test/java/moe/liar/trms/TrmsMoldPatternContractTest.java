package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Executes the shared v1 behavioral vectors as Extension-authority checks. */
class TrmsMoldPatternContractTest {
    private static final String CONTRACT_RESOURCE = "/mold-pattern-v1.properties";

    @Test
    void extensionAuthorityConformsToTheSharedV1MoldContract() throws IOException {
        Properties contract = loadContract();

        assertEquals(contractInt(contract, "format.version"), TrmsMoldPattern.FORMAT_VERSION);
        assertEquals(contractInt(contract, "interior.minimum"), TrmsMoldPattern.INTERIOR_MIN);
        assertEquals(contractInt(contract, "interior.maximum"), TrmsMoldPattern.INTERIOR_MAX);
        assertEquals(contractInt(contract, "interior.width"), TrmsMoldPattern.INTERIOR_WIDTH);
        assertEquals(contractInt(contract, "bit.count"), TrmsMoldPattern.BIT_COUNT);
        assertEquals(contractInt(contract, "byte.count"), TrmsMoldPattern.BYTE_COUNT);
        assertEquals(contractInt(contract, "index.1.1"), TrmsMoldPattern.carvingIndex(1, 1));
        assertEquals(contractInt(contract, "index.14.1"), TrmsMoldPattern.carvingIndex(14, 1));
        assertEquals(contractInt(contract, "index.1.14"), TrmsMoldPattern.carvingIndex(1, 14));
        assertEquals(contractInt(contract, "index.14.14"), TrmsMoldPattern.carvingIndex(14, 14));

        TrmsMoldPattern empty = TrmsMoldPattern.empty();
        assertCells(contract, "empty.legal", cell -> empty.canCarve(cell.x(), cell.z()), true);
        assertCells(contract, "empty.illegal", cell -> empty.canCarve(cell.x(), cell.z()), false);

        TrmsMoldPattern afterFirstCarve = empty.carve(8, 8);
        assertCells(contract, "after.8.8.legal", cell -> afterFirstCarve.canCarve(cell.x(), cell.z()), true);
        assertCells(contract, "after.8.8.illegal", cell -> afterFirstCarve.canCarve(cell.x(), cell.z()), false);
    }

    private static Properties loadContract() throws IOException {
        InputStream stream = TrmsMoldPatternContractTest.class.getResourceAsStream(CONTRACT_RESOURCE);
        assertNotNull(stream, "Shared TRMS mold-pattern v1 contract must be present in test resources");
        try (stream) {
            Properties contract = new Properties();
            contract.load(stream);
            return contract;
        }
    }

    private static int contractInt(Properties contract, String key) {
        String value = contract.getProperty(key);
        assertNotNull(value, () -> "Missing shared TRMS contract key: " + key);
        return Integer.parseInt(value);
    }

    private static void assertCells(Properties contract, String key, Predicate<Cell> predicate, boolean expected) {
        String encoded = contract.getProperty(key);
        assertNotNull(encoded, () -> "Missing shared TRMS contract key: " + key);
        Arrays.stream(encoded.split(";"))
                .map(Cell::parse)
                .forEach(cell -> {
                    if (expected) {
                        assertTrue(predicate.test(cell), () -> key + " must accept " + cell);
                    } else {
                        assertFalse(predicate.test(cell), () -> key + " must reject " + cell);
                    }
                });
    }

    private record Cell(int x, int z) {
        private static Cell parse(String encoded) {
            String[] parts = encoded.split(",", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid TRMS contract cell: " + encoded);
            }
            return new Cell(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }
}
