package moe.liar.trms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Registers an existing vanilla recipe type with Horizon's recipe sync.
 *
 * <p>Horizon exposes synchronization on its owned-type registration API, but
 * this recipe must retain {@link RecipeType#SMELTING} for furnace discovery.
 * The server-side sync bridge is intentionally isolated here because it is an
 * implementation service, not part of the Extension API.</p>
 */
final class TrmsRecipeContentSync {
    private static final String SYNC_CLASS = "moe.liar.horizon.network.recipe.RecipeContentSync";
    private static final Method REGISTER_SYNC_TYPE = findRegisterSyncType();

    private TrmsRecipeContentSync() {
    }

    static void registerVanillaSmeltingType() {
        try {
            REGISTER_SYNC_TYPE.invoke(null, RecipeType.SMELTING);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to register vanilla smelting recipes for Horizon sync", exception);
        }
    }

    private static Method findRegisterSyncType() {
        try {
            Class<?> syncClass = Class.forName(SYNC_CLASS);
            return syncClass.getMethod("registerSyncType", RecipeType.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            throw new IllegalStateException("Horizon does not expose recipe content synchronization", exception);
        }
    }
}
