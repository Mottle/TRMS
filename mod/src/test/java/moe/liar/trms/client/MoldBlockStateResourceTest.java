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
    void declaresEveryFacingWithTheCanonicalModelTurnWithoutEnumeratingCoolingStates() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/blockstates/mold.json")) {
            assertNotNull(resource, "mold blockstate resource must be packaged for the client");
            JsonObject blockState = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    ;
            assertFalse(blockState.has("variants"));
            var multipart = blockState.getAsJsonArray("multipart");
            assertNotNull(multipart);
            assertEquals(4, multipart.size());

            assertEquals(Set.of(
                    "south", "west", "north", "east"
            ), java.util.stream.StreamSupport.stream(multipart.spliterator(), false)
                    .map(element -> element.getAsJsonObject().getAsJsonObject("when")
                            .get("facing").getAsString())
                    .collect(java.util.stream.Collectors.toSet()));
            for (var element : multipart) {
                JsonObject entry = element.getAsJsonObject();
                String facing = entry.getAsJsonObject("when").get("facing").getAsString();
                JsonObject apply = entry.getAsJsonObject("apply");
                assertEquals("trms:block/mold", apply.get("model").getAsString());
                switch (facing) {
                    case "south" -> assertFalse(apply.has("y"));
                    case "west" -> assertEquals(90, apply.get("y").getAsInt());
                    case "north" -> assertEquals(180, apply.get("y").getAsInt());
                    case "east" -> assertEquals(270, apply.get("y").getAsInt());
                    default -> throw new AssertionError("unexpected facing " + facing);
                }
            }
        }
    }

    @Test
    void leavesTheInteriorBaseFaceToTheDynamicWorldMesh() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/assets/trms/models/block/mold.json")) {
            assertNotNull(resource, "mold block model resource must be packaged for the client");
            JsonObject model = JsonParser.parseString(
                    new String(resource.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject baseFaces = model.getAsJsonArray("elements").get(0)
                    .getAsJsonObject().getAsJsonObject("faces");
            assertFalse(baseFaces.has("up"),
                    "the dynamic cavity floor must be the only owner of the interior base face");
        }
    }
}
