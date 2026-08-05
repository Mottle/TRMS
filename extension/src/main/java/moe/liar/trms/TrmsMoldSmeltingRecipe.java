package moe.liar.trms;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/** Furnace recipe that fires a mold blank while carrying its carved pattern forward. */
final class TrmsMoldSmeltingRecipe extends SmeltingRecipe {
    static final MapCodec<TrmsMoldSmeltingRecipe> CODEC = AbstractCookingRecipe.cookingMapCodec(
            TrmsMoldSmeltingRecipe::new, 200);
    static final StreamCodec<RegistryFriendlyByteBuf, TrmsMoldSmeltingRecipe> STREAM_CODEC =
            AbstractCookingRecipe.cookingStreamCodec(TrmsMoldSmeltingRecipe::new);
    /*
     * SmeltingRecipe's API fixes the serializer type to SmeltingRecipe even
     * though this implementation is a subclass. Adapt both codecs at the
     * boundary so callers never receive a raw RecipeSerializer.
     */
    static final MapCodec<SmeltingRecipe> SERIALIZER_CODEC = CODEC.xmap(
            recipe -> recipe,
            recipe -> (TrmsMoldSmeltingRecipe) recipe
    );
    static final StreamCodec<RegistryFriendlyByteBuf, SmeltingRecipe> SERIALIZER_STREAM_CODEC =
            STREAM_CODEC.map(
                    recipe -> recipe,
                    recipe -> (TrmsMoldSmeltingRecipe) recipe
            );
    static final RecipeSerializer<SmeltingRecipe> SERIALIZER = new RecipeSerializer<>(
            SERIALIZER_CODEC, SERIALIZER_STREAM_CODEC);

    TrmsMoldSmeltingRecipe(Recipe.CommonInfo commonInfo,
                           AbstractCookingRecipe.CookingBookInfo bookInfo,
                           Ingredient ingredient,
                           ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }

    @Override
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        ItemStack result = super.assemble(input);
        TrmsMoldPattern pattern = patternFromInput(input.item(), TrmsContent.moldPatternComponent());
        if (pattern != null) {
            result.set(TrmsContent.moldPatternComponent(), pattern);
        }
        return result;
    }

    /** Reads the immutable carved pattern that should be carried into the result. */
    static TrmsMoldPattern patternFromInput(DataComponentGetter input,
                                            net.minecraft.core.component.DataComponentType<TrmsMoldPattern> component) {
        return input.get(component);
    }
}
