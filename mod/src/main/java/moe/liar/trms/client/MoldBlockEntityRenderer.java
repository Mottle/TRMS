package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Dynamic world renderer for the ceramic shell and currently solid inner voxels. */
public final class MoldBlockEntityRenderer implements BlockEntityRenderer<MoldBlockEntity, MoldBlockEntityRenderState> {
    private final MoldMeshBuilder.Cache meshCache = new MoldMeshBuilder.Cache(false);
    private final net.minecraft.client.resources.model.sprite.SpriteGetter sprites;

    public MoldBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public MoldBlockEntityRenderState createRenderState() {
        return new MoldBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(MoldBlockEntity blockEntity, MoldBlockEntityRenderState state,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
        MoldPattern pattern = blockEntity.pattern();
        Direction facing = MoldBlock.facing(blockEntity.getBlockState());
        state.pattern = pattern;
        state.revision = blockEntity.revision();
        state.fillMaterial = blockEntity.fillMaterial();
        state.facing = facing;
        state.showCarvingGuide = shouldShowCarvingGuide(blockEntity);
        state.carvingGuide = MoldCarvingGuide.Layout.EMPTY;
        if (state.showCarvingGuide) {
            state.carvingGuide = blockEntity.renderCache().carvingGuide(pattern, state.revision);
            state.showCarvingGuide = !state.carvingGuide.cells().isEmpty();
        }
        state.hoveredCarvingCell = state.showCarvingGuide
                ? hoveredCarvingCell(blockEntity, state.carvingGuide)
                : null;
        state.hoveredCarvingAlpha = state.hoveredCarvingCell == null || blockEntity.getLevel() == null
                ? 0
                : MoldCarvingGuide.hoverPulseAlpha(blockEntity.getLevel().getGameTime() + partialTick);
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = sprites.get(MoldMeshBuilder.TERRACOTTA_SPRITE);
        if (blockEntity.getLevel() instanceof BlockAndTintGetter level) {
            state.worldLighting = blockEntity.renderCache().worldLighting(
                    pattern,
                    state.revision,
                    state.lightCoords,
                    sprite,
                    facing,
                    () -> MoldMeshBuilder.captureWorldLighting(
                            level,
                            blockEntity.getBlockPos(),
                            blockEntity.getBlockState(),
                            pattern,
                            sprite,
                            facing
                    )
            );
        }
    }

    @Override
    public void submit(MoldBlockEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        MoldMeshBuilder.Mesh mesh = meshCache.get(
                state.pattern,
                state.revision,
                sprites.get(MoldMeshBuilder.TERRACOTTA_SPRITE)
        );
        poseStack.pushPose();
        // Keep the dynamic interior and every carving aid in the exact same
        // presentation space as the hand-held and dropped mold item.
        MoldMeshBuilder.rotateWorldPresentation(poseStack, state.facing);
        mesh.submitWorld(poseStack, collector, state.lightCoords, state.worldLighting);
        MoldFillVisual fillVisual = MoldFillVisual.forMaterial(state.fillMaterial);
        if (fillVisual != null && state.pattern.carvedCount() > 0) {
            MoldMeshBuilder.Mesh fillMesh = meshCache.getFill(
                    state.pattern,
                    sprites.get(MoldMeshBuilder.MOLTEN_STILL_SPRITE),
                    sprites.get(MoldMeshBuilder.MOLTEN_FLOW_SPRITE)
            );
            fillMesh.submitFullBrightWorld(poseStack, collector, fillVisual.color());
        }
        if (state.showCarvingGuide) {
            MoldCarvingGuide.submit(poseStack, collector, state.carvingGuide,
                    state.hoveredCarvingCell, state.hoveredCarvingAlpha);
        }
        poseStack.popPose();
    }

    private static boolean shouldShowCarvingGuide(MoldBlockEntity mold) {
        if (mold.isFilled()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || (!player.getMainHandItem().is(ItemTags.PICKAXES)
                && !player.getOffhandItem().is(ItemTags.PICKAXES))) {
            return false;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !hit.getBlockPos().equals(mold.getBlockPos())) {
            return false;
        }
        if (mold.getLevel() == null
                || !mold.getLevel().getBlockState(mold.getBlockPos().below()).is(Blocks.LODESTONE)) {
            return false;
        }
        return true;
    }

    private static MoldCarvingGuide.Cell hoveredCarvingCell(MoldBlockEntity mold, MoldCarvingGuide.Layout guide) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !hit.getBlockPos().equals(mold.getBlockPos())) {
            return null;
        }
        MoldCarvingGuide.Cell cell = MoldCarvingGuide.targetCell(
                hit.getLocation().x() - mold.getBlockPos().getX(),
                hit.getLocation().y() - mold.getBlockPos().getY(),
                hit.getLocation().z() - mold.getBlockPos().getZ(),
                MoldBlock.facing(mold.getBlockState())
        );
        return cell != null && guide.cells().contains(cell) ? cell : null;
    }
}
