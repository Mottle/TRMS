package moe.liar.trms;

import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.registrate.DeclarativeRegistrar;
import moe.liar.horizon.extension.registrate.entry.BlockEntry;
import moe.liar.horizon.extension.registrate.entry.RegistryEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
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

    static final BlockEntry<TrmsMoldBlock, TrmsMoldBlockEntity, AbstractContainerMenu> MOLD =
            REGISTRAR.block("mold", (properties, key) ->
                            new TrmsMoldBlock(properties, key, TrmsContent::moldBlockEntityType))
                    .properties(properties -> properties
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(1.25F, 6.0F)
                            .sound(SoundType.DECORATED_POT)
                            .lightLevel(state -> TrmsMoldFillMaterials.lightLevel(
                                    state.getValue(TrmsMoldBlock.FILLED)))
                            .noOcclusion())
                    .item(properties -> properties.stacksTo(1))
                    .blockEntity(TrmsMoldBlockEntity::new)
                    .register();

    static void register(ExtensionContext context) {
        REGISTRAR.registerAll(context);
    }

    static BlockEntityType<TrmsMoldBlockEntity> moldBlockEntityType() {
        return MOLD.blockEntity();
    }

    static DataComponentType<TrmsMoldPattern> moldPatternComponent() {
        return MOLD_PATTERN.get();
    }

    static Item moldItem() {
        return MOLD.item();
    }

    private TrmsContent() {
    }
}
