package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WeaponPartItemResourceTest {
    @Test
    void itemDefinitionUsesSeparateRenderersForGuiHandheldFrameAndGroundContexts() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/items/weapon_part.json")) {
            assertNotNull(resource, "weapon-part item definition must be packaged for the client");
            JsonObject model = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("model");

            assertEquals("minecraft:select", model.get("type").getAsString());
            JsonObject groundRenderer = model.getAsJsonArray("cases").get(0).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            JsonObject guiRenderer = model.getAsJsonArray("cases").get(1).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            JsonObject firstPersonRenderer = model.getAsJsonArray("cases").get(2).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            JsonObject thirdPersonRenderer = model.getAsJsonArray("cases").get(3).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            JsonObject fixedRenderer = model.getAsJsonArray("cases").get(4).getAsJsonObject()
                    .getAsJsonObject("model").getAsJsonObject("model");
            JsonObject fallbackRenderer = model.getAsJsonObject("fallback").getAsJsonObject("model");
            assertEquals("trms:weapon_part_ground_special", groundRenderer.get("type").getAsString());
            assertEquals("trms:weapon_part_gui_special", guiRenderer.get("type").getAsString());
            assertEquals("trms:weapon_part_first_person_special", firstPersonRenderer.get("type").getAsString());
            assertEquals("trms:weapon_part_third_person_special", thirdPersonRenderer.get("type").getAsString());
            assertEquals("trms:weapon_part_fixed_special", fixedRenderer.get("type").getAsString());
            assertEquals("trms:weapon_part_special", fallbackRenderer.get("type").getAsString());
        }
    }

    @Test
    void displayDefinitionUsesFortyFiveDegreeGuiPitchAndNativeHandheldTransforms() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/models/item/weapon_part.json")) {
            assertNotNull(resource, "weapon-part display definition must be packaged for the client");
            JsonObject display = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("display");

            assertEquals(45, display.getAsJsonObject("gui").getAsJsonArray("rotation").get(0).getAsInt());
            JsonObject firstPersonRightHand = display.getAsJsonObject("firstperson_righthand");
            assertEquals(0, firstPersonRightHand.getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(-90, firstPersonRightHand.getAsJsonArray("rotation").get(1).getAsInt());
            assertEquals(25, firstPersonRightHand.getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(1.13F, firstPersonRightHand.getAsJsonArray("translation").get(0).getAsFloat());
            assertEquals(3.2F, firstPersonRightHand.getAsJsonArray("translation").get(1).getAsFloat());
            assertEquals(1.13F, firstPersonRightHand.getAsJsonArray("translation").get(2).getAsFloat());
            assertEquals(0.68F, firstPersonRightHand.getAsJsonArray("scale").get(0).getAsFloat());

            JsonObject firstPersonLeftHand = display.getAsJsonObject("firstperson_lefthand");
            assertEquals(0, firstPersonLeftHand.getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(90, firstPersonLeftHand.getAsJsonArray("rotation").get(1).getAsInt());
            assertEquals(-25, firstPersonLeftHand.getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(1.13F, firstPersonLeftHand.getAsJsonArray("translation").get(0).getAsFloat());
            assertEquals(3.2F, firstPersonLeftHand.getAsJsonArray("translation").get(1).getAsFloat());
            assertEquals(1.13F, firstPersonLeftHand.getAsJsonArray("translation").get(2).getAsFloat());

            JsonObject thirdPersonRightHand = display.getAsJsonObject("thirdperson_righthand");
            JsonObject thirdPersonLeftHand = display.getAsJsonObject("thirdperson_lefthand");
            assertEquals(65, thirdPersonRightHand.getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(0, thirdPersonRightHand.getAsJsonArray("rotation").get(1).getAsInt());
            assertEquals(4, thirdPersonRightHand.getAsJsonArray("translation").get(1).getAsInt());
            assertEquals(2, thirdPersonRightHand.getAsJsonArray("translation").get(2).getAsInt());
            assertEquals(thirdPersonRightHand, thirdPersonLeftHand,
                    "both third-person hands must use the same calibrated local placement");

            JsonObject fixed = display.getAsJsonObject("fixed");
            assertEquals(0, fixed.getAsJsonArray("rotation").get(0).getAsInt());
            assertEquals(0, fixed.getAsJsonArray("rotation").get(1).getAsInt());
            assertEquals(0, fixed.getAsJsonArray("rotation").get(2).getAsInt());
            assertEquals(0.65F, fixed.getAsJsonArray("scale").get(0).getAsFloat());
        }
    }
}
