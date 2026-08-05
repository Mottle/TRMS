package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TrmsMoldBlankRecipeTest {
    @Test
    void recipeUsesNineClayBallsAndProducesOneBlank() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/data/trms/recipe/mold_blank.json")) {
            assertNotNull(resource, "mold blank recipe must be packaged by the Extension");
            var recipe = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals(3, recipe.getAsJsonArray("pattern").size());
            recipe.getAsJsonArray("pattern").forEach(row -> assertEquals("CCC", row.getAsString()));
            assertEquals("minecraft:clay_ball", recipe.getAsJsonObject("key")
                    .get("C").getAsString());
            assertEquals("trms:mold_blank", recipe.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
