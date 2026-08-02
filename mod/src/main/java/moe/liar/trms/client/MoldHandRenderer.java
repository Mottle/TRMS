package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** Adds the otherwise omitted player hand behind the first-person ceramic mold. */
public final class MoldHandRenderer {
    private MoldHandRenderer() {}

    /**
     * Vanilla 26.1 only renders a visible player hand for an empty hand and maps.
     * A mold is a broad, flat held object, so explicitly submit the same arm pose
     * that vanilla uses before it submits an item.  The event is intentionally not
     * cancelled: {@code ItemInHandRenderer} still renders the special mold model.
     */
    public static void renderHeldMoldHand(RenderHandEvent event) {
        if (!event.getItemStack().is(TrmsClientMod.MOLD_ITEM.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        renderPlayerHand(event, player, arm);
    }

    private static void renderPlayerHand(RenderHandEvent event, AbstractClientPlayer player, HumanoidArm arm) {
        boolean rightArm = arm == HumanoidArm.RIGHT;
        float direction = rightArm ? 1.0f : -1.0f;
        float swingRoot = Mth.sqrt(event.getSwingProgress());
        float xOffset = -0.3f * Mth.sin(swingRoot * (float) Math.PI);
        float yOffset = 0.4f * Mth.sin(swingRoot * (float) (Math.PI * 2.0));
        float zOffset = -0.4f * Mth.sin(event.getSwingProgress() * (float) Math.PI);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
                direction * (xOffset + 0.64000005f),
                yOffset - 0.6f - event.getEquipProgress() * 0.6f,
                zOffset - 0.71999997f
        );
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(direction * 45.0f));
        float swingRotation = Mth.sin(event.getSwingProgress() * event.getSwingProgress() * (float) Math.PI);
        float swingYaw = Mth.sin(swingRoot * (float) Math.PI);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(direction * swingYaw * 70.0f));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(direction * swingRotation * -20.0f));
        poseStack.translate(direction * -1.0f, 3.6f, 3.5f);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(direction * 120.0f));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(200.0f));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(direction * -135.0f));
        poseStack.translate(direction * 5.6f, 0.0f, 0.0f);

        AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        if (rightArm) {
            renderer.renderRightHand(
                    poseStack,
                    event.getSubmitNodeCollector(),
                    event.getPackedLight(),
                    player.getSkin().body().texturePath(),
                    player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),
                    player
            );
        } else {
            renderer.renderLeftHand(
                    poseStack,
                    event.getSubmitNodeCollector(),
                    event.getPackedLight(),
                    player.getSkin().body().texturePath(),
                    player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                    player
            );
        }
        poseStack.popPose();
    }
}
