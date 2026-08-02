package moe.liar.trms.client;

import moe.liar.trms.common.TrmsProtocol;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only NeoForge entrypoint paired with the Horizon TRMS Extension. */
@Mod(value = TrmsClientMod.MOD_ID, dist = Dist.CLIENT)
public final class TrmsClientMod {
    public static final String MOD_ID = TrmsProtocol.NAMESPACE;
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<MoldPattern>> MOLD_PATTERN =
            COMPONENTS.registerComponentType("mold_pattern", builder -> builder
                    .persistent(MoldPattern.CODEC)
                    .networkSynchronized(MoldPattern.STREAM_CODEC));

    public static final DeferredHolder<Block, MoldBlock> MOLD = BLOCKS.registerBlock(
            "mold",
            MoldBlock::new,
            properties -> properties
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .sound(SoundType.DECORATED_POT)
                    .strength(1.25F, 6.0F)
                    // Keep the client-side registry definition aligned with the
                    // Extension's filled-state light emission. Server light
                    // packets remain authoritative, while this also keeps local
                    // light recalculation and state-dependent rendering coherent.
                    .lightLevel(state -> state.getValue(MoldBlock.FILLED) ? 15 : 0)
                    .noOcclusion()
    );
    public static final DeferredHolder<Item, BlockItem> MOLD_ITEM = ITEMS.registerItem(
            "mold",
            properties -> new BlockItem(MOLD.get(), properties
                    .stacksTo(1)
                    .useBlockDescriptionPrefix())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MoldBlockEntity>> MOLD_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mold", () -> new BlockEntityType<>(MoldBlockEntity::new, MOLD.get()));

    private static final Logger LOGGER = LoggerFactory.getLogger("TRMS");

    public TrmsClientMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TrmsClientRendering::registerRenderers);
        modBus.addListener(TrmsClientRendering::registerSpecialModelRenderers);
        modBus.addListener(TrmsHandshake::registerPayloads);
        modBus.addListener(TrmsHandshake::registerClientHandlers);
        NeoForge.EVENT_BUS.addListener(TrmsCarvingInput::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(MoldHandRenderer::renderHeldMoldHand);
        LOGGER.info("TRMS client mod initialized (protocol v{})", TrmsProtocol.VERSION);
    }
}
