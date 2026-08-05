package moe.liar.trms.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/** Client mirror for the Extension's pattern-preserving furnace recipe. */
final class ClientMoldSmeltingRecipe extends SmeltingRecipe {
    static final MapCodec<ClientMoldSmeltingRecipe> CODEC = AbstractCookingRecipe.cookingMapCodec(
            ClientMoldSmeltingRecipe::new, 200);
    static final StreamCodec<RegistryFriendlyByteBuf, ClientMoldSmeltingRecipe> STREAM_CODEC =
            AbstractCookingRecipe.cookingStreamCodec(ClientMoldSmeltingRecipe::new);
    static final MapCodec<SmeltingRecipe> SERIALIZER_CODEC = CODEC.xmap(
            recipe -> recipe,
            recipe -> (ClientMoldSmeltingRecipe) recipe
    );
    static final StreamCodec<RegistryFriendlyByteBuf, SmeltingRecipe> SERIALIZER_STREAM_CODEC =
            STREAM_CODEC.map(
                    recipe -> recipe,
                    recipe -> (ClientMoldSmeltingRecipe) recipe
            );
    static final RecipeSerializer<SmeltingRecipe> SERIALIZER = new RecipeSerializer<>(
            SERIALIZER_CODEC, SERIALIZER_STREAM_CODEC);

    ClientMoldSmeltingRecipe(Recipe.CommonInfo commonInfo,
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
        MoldPattern pattern = input.item().get(TrmsClientMod.MOLD_PATTERN.get());
        if (pattern != null) {
            result.set(TrmsClientMod.MOLD_PATTERN.get(), pattern);
        }
        return result;
    }
}
