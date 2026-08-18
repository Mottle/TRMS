package moe.liar.trms;

import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.ExtensionConcurrencyContext;
import moe.liar.horizon.extension.ExtensionContext;
import moe.liar.horizon.extension.event.PlayerQuitEvent;
import moe.liar.horizon.extension.network.PayloadRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/** Registers and executes the server-authoritative play-phase mold carving request. */
final class TrmsMoldCarvingNetwork {
    private static final double VANILLA_BLOCK_INTERACTION_BUFFER = 1.0D;
    private static final TrmsCarvingRateLimiter CARVING_RATE_LIMITER = new TrmsCarvingRateLimiter();

    private TrmsMoldCarvingNetwork() {
    }

    static void register(ExtensionContext context) {
        context.network().registerServerboundPayload(
                TrmsCarveMoldPayload.TYPE,
                TrmsCarveMoldPayload.STREAM_CODEC,
                ConnectionProtocol.PLAY,
                TrmsProtocol.CARVING_TRANSPORT_VERSION,
                PayloadRequirement.REQUIRED,
                TrmsCarveMoldPayload.class,
                (listener, payload) -> receive(context.concurrency(), listener, payload)
        );
        context.events().listen(PlayerQuitEvent.class,
                event -> CARVING_RATE_LIMITER.remove(event.player().getUUID()));
    }

    /**
     * Transfers the immutable request to the player's owner before its
     * untrusted block position is used to select a world region. Once the
     * normal interaction-range check passes, Horizon's nearby-region invariant
     * makes the target mold owner-local; {@code ownsBlock} remains a defensive
     * assertion rather than an assumption about client input.
     */
    private static void receive(ExtensionConcurrencyContext concurrency,
                                ServerCommonPacketListenerImpl listener,
                                TrmsCarveMoldPayload payload) {
        if (!(listener instanceof ServerGamePacketListenerImpl gameListener)) {
            return;
        }
        ServerPlayer player = gameListener.getPlayer();
        concurrency.submitOnRegion(player,
                () -> carveOnPlayerOwner(concurrency, player, payload));
    }

    /**
     * Revalidates the complete interaction on the player-owner thread. Client
     * coordinates, tools, revision, proximity, and permissions are never
     * trusted merely because a payload reached this method.
     */
    private static void carveOnPlayerOwner(ExtensionConcurrencyContext concurrency,
                                           ServerPlayer player,
                                           TrmsCarveMoldPayload payload) {
        int cellX = payload.cellX();
        int cellZ = payload.cellZ();
        if (!TrmsMoldPattern.isCarvableCell(cellX, cellZ)) {
            return;
        }

        ServerLevel level = player.level();
        BlockPos moldPos = payload.moldPos();
        if (!player.isWithinBlockInteractionRange(moldPos, VANILLA_BLOCK_INTERACTION_BUFFER)) {
            return;
        }
        if (!concurrency.ownsBlock(level, moldPos)) {
            return;
        }
        if (!player.mayInteract(level, moldPos)) {
            return;
        }

        ItemStack tool = player.getItemInHand(payload.hand());
        if (!tool.is(ItemTags.PICKAXES)
                || !player.mayUseItemAt(moldPos, Direction.UP, tool)) {
            return;
        }
        if (!level.getBlockState(moldPos).is(TrmsContent.MOLD_BLANK.block())
                || !(level.getBlockEntity(moldPos) instanceof TrmsMoldBlockEntity mold)
                || !TrmsMoldBlock.hasLodestoneCarvingBase(level, moldPos)
                || mold.fillMaterial().isPresent()
                || mold.revision() != payload.expectedRevision()) {
            return;
        }

        long gameTick = TrmsServerClock.currentTick();
        if (!mold.pattern().canCarve(cellX, cellZ)
                || !CARVING_RATE_LIMITER.tryAcquire(player.getUUID(), gameTick)
                || !mold.carve(cellX, cellZ)) {
            return;
        }

        if (!player.isCreative()) {
            tool.hurtAndBreak(1, player, payload.hand());
        }
    }
}
