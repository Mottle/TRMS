package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MoldBlankResourceTest {
    @Test
    void exposesAllFourFacingsAndClayShell() throws IOException {
        try (InputStream state = getClass().getResourceAsStream("/assets/trms/blockstates/mold_blank.json");
             InputStream model = getClass().getResourceAsStream("/assets/trms/models/block/mold_blank.json")) {
            assertNotNull(state);
            assertNotNull(model);
            assertEquals(4, JsonParser.parseString(new String(state.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("multipart").size());
            assertEquals("minecraft:block/clay", JsonParser.parseString(
                    new String(model.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures").get("ceramic").getAsString());
        }
    }

    @Test
    void blankItemUsesClaySpecialRendererForFirstPerson() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/items/mold_blank.json")) {
            assertNotNull(resource);
            var model = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("model");
            var firstPerson = model.getAsJsonArray("cases").get(2).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            assertEquals("trms:mold_blank_first_person_special", firstPerson.get("type").getAsString());
        }
    }
}
