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
    void declaresEveryFacingAndFillStateWithTheCanonicalModelTurn() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/blockstates/mold.json")) {
            assertNotNull(resource, "mold blockstate resource must be packaged for the client");
            JsonObject variants = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonObject("variants");

            assertEquals(Set.of(
                    "facing=south,filled=false", "facing=south,filled=true",
                    "facing=west,filled=false", "facing=west,filled=true",
                    "facing=north,filled=false", "facing=north,filled=true",
                    "facing=east,filled=false", "facing=east,filled=true"
            ), variants.keySet());
            for (String filled : new String[] {"false", "true"}) {
                assertFalse(variants.getAsJsonObject("facing=south,filled=" + filled).has("y"));
                assertEquals(90, variants.getAsJsonObject("facing=west,filled=" + filled).get("y").getAsInt());
                assertEquals(180, variants.getAsJsonObject("facing=north,filled=" + filled).get("y").getAsInt());
                assertEquals(270, variants.getAsJsonObject("facing=east,filled=" + filled).get("y").getAsInt());
            }
        }
    }
}
