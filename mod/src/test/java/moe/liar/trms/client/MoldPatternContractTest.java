package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Predicate;
import moe.liar.trms.common.MoldPersistence;
import org.junit.jupiter.api.Test;

/** Executes the shared v1 behavioral vectors as client-side prediction checks. */
class MoldPatternContractTest {
    private static final String CONTRACT_RESOURCE = "/mold-pattern-v1.properties";

    @Test
    void clientPredictionConformsToTheSharedV1MoldContract() throws IOException {
        Properties contract = loadContract();

        assertEquals(contractInt(contract, "format.version"), MoldPersistence.FORMAT_VERSION);
        assertEquals(contractInt(contract, "interior.width"), MoldPattern.INNER_SIZE);
        assertEquals(contractInt(contract, "bit.count"), MoldPattern.BIT_COUNT);
        assertEquals(contractInt(contract, "byte.count"), MoldPattern.BYTE_COUNT);
        assertEquals(contractInt(contract, "index.1.1"), MoldPattern.index(1, 1));
        assertEquals(contractInt(contract, "index.14.1"), MoldPattern.index(14, 1));
        assertEquals(contractInt(contract, "index.1.14"), MoldPattern.index(1, 14));
        assertEquals(contractInt(contract, "index.14.14"), MoldPattern.index(14, 14));

        assertCells(contract, "empty.legal", cell -> MoldPattern.EMPTY.canCarveAt(cell.x(), cell.z()), true);
        assertCells(contract, "empty.illegal", cell -> MoldPattern.EMPTY.canCarveAt(cell.x(), cell.z()), false);

        MoldPattern afterFirstCarve = MoldPattern.EMPTY.predictCarve(8, 8).orElseThrow();
        assertCells(contract, "after.8.8.legal", cell -> afterFirstCarve.canCarveAt(cell.x(), cell.z()), true);
        assertCells(contract, "after.8.8.illegal", cell -> afterFirstCarve.canCarveAt(cell.x(), cell.z()), false);
    }

    private static Properties loadContract() throws IOException {
        InputStream stream = MoldPatternContractTest.class.getResourceAsStream(CONTRACT_RESOURCE);
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
