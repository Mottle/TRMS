package moe.liar.trms;

import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.registrate.DeclarativeRegistrar;
import moe.liar.horizon.extension.registrate.entry.BlockEntry;
import moe.liar.horizon.extension.registrate.entry.ItemEntry;
import moe.liar.horizon.extension.registrate.entry.RecipeEntry;
import moe.liar.horizon.extension.registrate.entry.RegistryEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;

/** Explicit, one-content-entry-at-a-time registration for the TRMS mold loop. */
final class TrmsContent {
    private static final DeclarativeRegistrar REGISTRAR = DeclarativeRegistrar.create(TrmsProtocol.NAMESPACE);

    static final RegistryEntry<DataComponentType<TrmsMoldPattern>> MOLD_PATTERN =
            REGISTRAR.<TrmsMoldPattern>dataComponent("mold_pattern")
                    .persistent(TrmsMoldPattern.CODEC)
                    .networkSynchronized(TrmsMoldPattern.STREAM_CODEC)
                    .register();

    static final RegistryEntry<DataComponentType<TrmsWeaponPart>> WEAPON_PART =
            REGISTRAR.<TrmsWeaponPart>dataComponent("weapon_part")
                    .persistent(TrmsWeaponPart.CODEC)
                    .networkSynchronized(TrmsWeaponPart.STREAM_CODEC)
                    .register();

    static final RegistryEntry<DataComponentType<TrmsAssembledWeapon>> ASSEMBLED_WEAPON =
            REGISTRAR.<TrmsAssembledWeapon>dataComponent("assembled_weapon")
                    .persistent(TrmsAssembledWeapon.CODEC)
                    .networkSynchronized(TrmsAssembledWeapon.STREAM_CODEC)
                    .register();

    static final ItemEntry<Item> WEAPON_PART_ITEM = REGISTRAR.item("weapon_part")
            .properties(properties -> properties.stacksTo(1))
            .register();

    static final ItemEntry<TrmsAssembledWeaponItem> ASSEMBLED_WEAPON_ITEM = REGISTRAR.item(
            "assembled_weapon", TrmsAssembledWeaponItem::new)
            .properties(properties -> properties.stacksTo(1))
            .register();

    /** Serializer used by the mold-from-blank recipe JSON and sync payload. */
    static final RecipeEntry<SmeltingRecipe> MOLD_SMELTING_SERIALIZER =
            REGISTRAR.<SmeltingRecipe>recipe("mold_smelting")
                    .codec(TrmsMoldSmeltingRecipe.SERIALIZER_CODEC,
                            TrmsMoldSmeltingRecipe.SERIALIZER_STREAM_CODEC)
                    .registerTypeAndSerializer();

    static final BlockEntry<TrmsMoldBlock, BlockEntity, AbstractContainerMenu> MOLD =
            REGISTRAR.block("mold", (properties, key) ->
                            new TrmsMoldBlock(properties, key, TrmsContent::moldBlockEntityType))
                    .properties(properties -> properties
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(1.25F, 6.0F)
                            .sound(SoundType.DECORATED_POT)
                            .lightLevel(state -> TrmsMoldFillMaterials.lightLevel(
                                    state.getValue(TrmsMoldBlock.FILLED),
                                    state.getValue(TrmsMoldBlock.COOLING_STAGE)))
                            .noOcclusion())
                    .item(properties -> properties.stacksTo(1))
                    .register();

    static final BlockEntry<TrmsMoldBlock, BlockEntity, AbstractContainerMenu> MOLD_BLANK =
            REGISTRAR.block("mold_blank", (properties, key) ->
                            new TrmsMoldBlock(properties, key, TrmsContent::moldBlockEntityType))
                    .properties(properties -> properties
                            .mapColor(MapColor.CLAY)
                            .strength(1.25F, 6.0F)
                            .sound(SoundType.GRAVEL)
                            .noOcclusion())
                    .item(properties -> properties.stacksTo(1))
                    .register();

    static final RegistryEntry<BlockEntityType<TrmsMoldBlockEntity>> MOLD_BLOCK_ENTITY =
            REGISTRAR.blockEntity("mold", TrmsMoldBlockEntity::new)
                    .validBlocks(MOLD::block, MOLD_BLANK::block)
                    .register();

    static void register(ExtensionContext context) {
        REGISTRAR.registerAll(context);
        // The recipe deliberately keeps vanilla RecipeType.SMELTING so furnaces
        // can discover it. Horizon's public registrar can only sync newly-owned
        // recipe types (and registering the vanilla singleton under a second id
        // is rejected by Minecraft's registry), so bridge the existing type to
        // Horizon's content-sync set after all owned content is registered.
        TrmsRecipeContentSync.registerVanillaSmeltingType();
    }

    static BlockEntityType<TrmsMoldBlockEntity> moldBlockEntityType() {
        return MOLD_BLOCK_ENTITY.get();
    }

    static DataComponentType<TrmsMoldPattern> moldPatternComponent() {
        return MOLD_PATTERN.get();
    }

    static Item moldItem() {
        return MOLD.item();
    }

    static Item moldBlankItem() {
        return MOLD_BLANK.item();
    }

    static DataComponentType<TrmsWeaponPart> weaponPartComponent() {
        return WEAPON_PART.get();
    }

    static Item weaponPartItem() {
        return WEAPON_PART_ITEM.get();
    }

    static DataComponentType<TrmsAssembledWeapon> assembledWeaponComponent() {
        return ASSEMBLED_WEAPON.get();
    }

    static Item assembledWeaponItem() {
        return ASSEMBLED_WEAPON_ITEM.get();
    }

    private TrmsContent() {
    }
}
