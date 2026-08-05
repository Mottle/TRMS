package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TrmsMoldSmeltingRecipeTest {
    private static DataComponentType<TrmsMoldPattern> testPatternComponent;

    @BeforeAll
    static void initializeMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testPatternComponent = DataComponentType.<TrmsMoldPattern>builder()
                .persistent(TrmsMoldPattern.CODEC)
                .networkSynchronized(TrmsMoldPattern.STREAM_CODEC)
                .build();
    }

    @Test
    void smeltsOneBlankIntoOneMoldInAStandardFurnace() throws IOException {
        try (InputStream resource = getClass().getResourceAsStream("/data/trms/recipe/mold_from_blank.json")) {
            assertNotNull(resource, "mold smelting recipe must be packaged by the Extension");
            var recipe = JsonParser.parseString(new String(resource.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("trms:mold_smelting", recipe.get("type").getAsString());
            assertEquals("trms:mold_blank", recipe.get("ingredient").getAsString());
            assertEquals("trms:mold", recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(200, recipe.get("cookingtime").getAsInt());
        }
    }

    @Test
    void carriesTheCompleteCarvingPatternToTheSmeltedMold() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty()
                .carve(6, 6)
                .carve(7, 6)
                .carve(8, 6);
        DataComponentMap input = DataComponentMap.builder()
                .set(testPatternComponent, pattern)
                .build();

        assertEquals(pattern, TrmsMoldSmeltingRecipe.patternFromInput(input, testPatternComponent));

        // The recipe reads the immutable value; it never mutates the input.
        assertEquals(pattern, input.get(testPatternComponent));
    }

    @Test
    void leavesTheResultUnmodifiedWhenTheBlankHasNoCarvingPattern() {
        DataComponentMap input = DataComponentMap.EMPTY;

        org.junit.jupiter.api.Assertions.assertNull(
                TrmsMoldSmeltingRecipe.patternFromInput(input, testPatternComponent));
    }

    @Test
    void syncBridgeKeepsTheVanillaSmeltingTypeInsteadOfRegisteringAnAlias() {
        assertDoesNotThrow(TrmsRecipeContentSync::registerVanillaSmeltingType);
        assertEquals("minecraft:smelting", BuiltInRegistries.RECIPE_TYPE.getKey(RecipeType.SMELTING).toString());
    }
}
