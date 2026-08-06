package moe.liar.trms.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Detects the crouch + casting + stick gesture without replacing ordinary use otherwise. */
final class TrmsAssemblyInput {
    private TrmsAssemblyInput() {
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryStart(event.getEntity(), event.getLevel(), event.getHand(),
                result -> {
                    event.setCancellationResult(result);
                    event.setCanceled(true);
                })) {
            // The callback performs cancellation after the packet is queued.
        }
    }

    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        tryStart(event.getEntity(), event.getLevel(), event.getHand(),
                result -> {
                    event.setCancellationResult(result);
                    event.setCanceled(true);
                });
    }

    static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        tryStart(event.getEntity(), event.getLevel(), InteractionHand.MAIN_HAND, result -> {
            // RightClickEmpty is cancellable but has no cancellation result setter on all NeoForge mappings.
        });
    }

    private static boolean tryStart(Player player, Level level, InteractionHand hand, java.util.function.Consumer<InteractionResult> cancel) {
        if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !player.isCrouching()
                || player.isSpectator() || !player.getMainHandItem().is(TrmsClientMod.WEAPON_PART_ITEM.get())
                || player.getMainHandItem().get(TrmsClientMod.WEAPON_PART.get()) == null
                || !player.getOffhandItem().is(net.minecraft.world.item.Items.STICK)) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new AssemblyStartPayload());
        cancel.accept(InteractionResult.SUCCESS);
        return true;
    }
}
