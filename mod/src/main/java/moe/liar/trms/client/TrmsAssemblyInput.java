package moe.liar.trms.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.InputEvent;

/** Detects the crouch + casting + stick gesture before vanilla resolves the targeted interaction. */
final class TrmsAssemblyInput {
    private TrmsAssemblyInput() {
    }

    /**
     * Opens the assembly flow from the use key itself, before vanilla chooses
     * a block, entity, or empty-space interaction path.  The three
     * {@code PlayerInteractEvent} variants are not equivalent: only one may
     * fire depending on the hit result, and a preceding interaction can skip
     * the later variants entirely.
     */
    static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()
                || player.isSpectator() || !player.getMainHandItem().is(TrmsClientMod.WEAPON_PART_ITEM.get())
                || player.getMainHandItem().get(TrmsClientMod.WEAPON_PART.get()) == null
                || !player.getOffhandItem().is(net.minecraft.world.item.Items.STICK)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new AssemblyStartPayload());
        // The combination screen follows the server's validation response.
        // Suppressing vanilla use prevents an unrelated block or item action
        // (and the misleading hand swing) during that round trip.
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
