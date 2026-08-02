package moe.liar.trms.client;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Selects the visible carving cell on the client and suppresses the ordinary
 * use-item packet only after a dedicated serverbound request was sent.
 */
final class TrmsCarvingInput {
    private TrmsCarvingInput() {
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide() || !event.getItemStack().is(ItemTags.PICKAXES)) {
            return;
        }

        BlockPos moldPos = event.getPos();
        BlockState moldState = level.getBlockState(moldPos);
        if (!moldState.is(TrmsClientMod.MOLD.get())
                || !level.getBlockState(moldPos.below()).is(Blocks.LODESTONE)
                || !(level.getBlockEntity(moldPos) instanceof MoldBlockEntity mold)
                || mold.isFilled()) {
            return;
        }

        MoldCarvingGuide.Cell cell = MoldCarvingGuide.targetCell(
                event.getHitVec().getLocation().x() - moldPos.getX(),
                event.getHitVec().getLocation().y() - moldPos.getY(),
                event.getHitVec().getLocation().z() - moldPos.getZ(),
                MoldBlock.facing(moldState)
        );
        if (cell == null || !mold.pattern().canCarveAt(cell.x(), cell.z())) {
            return;
        }

        ClientPacketDistributor.sendToServer(new CarveMoldPayload(
                moldPos,
                (byte) cell.x(),
                (byte) cell.z(),
                event.getHand(),
                mold.revision()
        ));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
