package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MoldItemResourceTest {
    @Test
    void thirdPersonMoldUsesACompactHandAnchoredScale() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/models/item/mold.json")) {
            assertNotNull(resource, "mold item model must be packaged for the client");
            JsonObject display = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("display");

            JsonObject rightHand = display.getAsJsonObject("thirdperson_righthand");
            JsonObject leftHand = display.getAsJsonObject("thirdperson_lefthand");
            assertEquals(0.5F, rightHand.getAsJsonArray("scale").get(0).getAsFloat());
            assertEquals(0.5F, leftHand.getAsJsonArray("scale").get(0).getAsFloat());
            assertEquals(3, rightHand.getAsJsonArray("translation").get(1).getAsInt());
            assertEquals(3, leftHand.getAsJsonArray("translation").get(1).getAsInt());
        }
    }

    @Test
    void firstPersonMoldIsAnchoredLowInTheHand() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/models/item/mold.json")) {
            assertNotNull(resource, "mold item model must be packaged for the client");
            JsonObject display = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("display");

            JsonObject rightHand = display.getAsJsonObject("firstperson_righthand");
            JsonObject leftHand = display.getAsJsonObject("firstperson_lefthand");
            assertEquals(-8, rightHand.getAsJsonArray("translation").get(2).getAsInt());
            assertEquals(-8, leftHand.getAsJsonArray("translation").get(2).getAsInt());
            assertEquals(-3, rightHand.getAsJsonArray("translation").get(1).getAsInt());
            assertEquals(-3, leftHand.getAsJsonArray("translation").get(1).getAsInt());
            assertEquals(0.7F, rightHand.getAsJsonArray("scale").get(0).getAsFloat());
            assertEquals(0.7F, leftHand.getAsJsonArray("scale").get(0).getAsFloat());
        }
    }

    @Test
    void itemDefinitionUsesTheDedicatedFirstPersonMoldRenderer() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/items/mold.json")) {
            assertNotNull(resource, "mold item definition must be packaged for the client");
            JsonObject model = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("model");

            JsonObject firstPersonRenderer = model.getAsJsonArray("cases").get(2).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            assertEquals("trms:mold_first_person_special", firstPersonRenderer.get("type").getAsString());
        }
    }
}
