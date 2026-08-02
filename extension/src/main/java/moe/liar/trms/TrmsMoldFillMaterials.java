package moe.liar.trms;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import moe.liar.trms.common.MoldCooling;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-side gameplay bindings for extensible, side-neutral fill material IDs. */
final class TrmsMoldFillMaterials {

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(MoldFillMaterial.COPPER, Items.COPPER_INGOT),
            new Definition(MoldFillMaterial.IRON, Items.IRON_INGOT)
    );

    static {
        Set<MoldFillMaterial> materials = new HashSet<>();
        Set<Item> ingredients = new HashSet<>();
        for (Definition definition : DEFINITIONS) {
            if (!materials.add(definition.material())) {
                throw new IllegalStateException("Duplicate TRMS mold fill material: " + definition.material());
            }
            if (!ingredients.add(definition.ingredient())) {
                throw new IllegalStateException("Duplicate TRMS mold fill ingredient: " + definition.ingredient());
            }
        }
    }

    private TrmsMoldFillMaterials() {
    }

    /** Resolves a supported demonstration ingot without accepting arbitrary client material IDs. */
    static Optional<Definition> forIngredient(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return forIngredient(stack.getItem());
    }

    static Optional<Definition> forIngredient(Item item) {
        Objects.requireNonNull(item, "item");
        return DEFINITIONS.stream().filter(definition -> definition.ingredient() == item).findFirst();
    }

    static int lightLevel(boolean filled, int coolingStage) {
        return filled ? MoldCooling.lightLevel(coolingStage) : 0;
    }

    /** One authoritative server gameplay binding; additional materials append another definition. */
    record Definition(MoldFillMaterial material, Item ingredient) {
        Definition {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(ingredient, "ingredient");
        }
    }
}
