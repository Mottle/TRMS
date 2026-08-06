package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AssembledWeaponItemResourceTest {
    @Test
    void itemDefinitionUsesDedicatedRenderersForAllDisplayContexts() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/items/assembled_weapon.json")) {
            assertNotNull(resource, "assembled-weapon item definition must be packaged for the client");
            JsonObject model = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("model");

            assertEquals("minecraft:select", model.get("type").getAsString());
            assertEquals("trms:assembled_weapon_ground_special", rendererType(model, 0));
            assertEquals("trms:assembled_weapon_gui_special", rendererType(model, 1));
            assertEquals("trms:assembled_weapon_first_person_special", rendererType(model, 2));
            assertEquals("trms:assembled_weapon_third_person_special", rendererType(model, 3));
            assertEquals("trms:assembled_weapon_fixed_special", rendererType(model, 4));
            assertEquals("trms:assembled_weapon_special", model.getAsJsonObject("fallback")
                    .getAsJsonObject("model").get("type").getAsString());
        }
    }

    @Test
    void displayDefinitionDeclaresEveryTransformUsedByTheRenderer() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/models/item/assembled_weapon.json")) {
            assertNotNull(resource, "assembled-weapon display definition must be packaged for the client");
            JsonObject display = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("display");

            assertNotNull(display.getAsJsonObject("gui"));
            assertNotNull(display.getAsJsonObject("firstperson_righthand"));
            assertNotNull(display.getAsJsonObject("firstperson_lefthand"));
            assertNotNull(display.getAsJsonObject("thirdperson_righthand"));
            assertNotNull(display.getAsJsonObject("thirdperson_lefthand"));
            assertNotNull(display.getAsJsonObject("ground"));
            assertNotNull(display.getAsJsonObject("fixed"));
            assertEquals(45, display.getAsJsonObject("gui").getAsJsonArray("rotation").get(0).getAsInt());
        }
    }

    private static String rendererType(JsonObject model, int index) {
        return model.getAsJsonArray("cases").get(index).getAsJsonObject()
                .getAsJsonObject("model").getAsJsonObject("model").get("type").getAsString();
    }
}
