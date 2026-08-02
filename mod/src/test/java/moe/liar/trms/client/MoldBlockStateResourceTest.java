package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MoldBlockStateResourceTest {
    @Test
    void declaresEveryHorizontalFacingWithTheCanonicalModelTurn() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/blockstates/mold.json")) {
            assertNotNull(resource, "mold blockstate resource must be packaged for the client");
            JsonObject variants = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonObject("variants");

            assertEquals(Set.of("facing=south", "facing=west", "facing=north", "facing=east"), variants.keySet());
            assertFalse(variants.getAsJsonObject("facing=south").has("y"));
            assertEquals(90, variants.getAsJsonObject("facing=west").get("y").getAsInt());
            assertEquals(180, variants.getAsJsonObject("facing=north").get("y").getAsInt());
            assertEquals(270, variants.getAsJsonObject("facing=east").get("y").getAsInt());
        }
    }
}
