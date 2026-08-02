package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

/** NeoForge 26.1 replacement for the removed legacy BlockEntityWithoutLevelRenderer path. */
public final class MoldSpecialModelRenderer implements SpecialModelRenderer<ItemStack> {
    private final MoldMeshBuilder.Cache meshCache = new MoldMeshBuilder.Cache(true);
    private final boolean preCenteredItemGeometry;

    /** Creates the established hand, display-frame, and fallback item renderer. */
    public MoldSpecialModelRenderer() {
        this(true);
    }

    private MoldSpecialModelRenderer(boolean preCenteredItemGeometry) {
        this.preCenteredItemGeometry = preCenteredItemGeometry;
    }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int seed) {
        MoldPattern pattern = stack.getOrDefault(TrmsClientMod.MOLD_PATTERN.get(), MoldPattern.EMPTY);
        MoldMeshBuilder.Mesh mesh = meshCache.get(pattern, 0L, MoldMeshBuilder.currentTerracottaSprite());
        if (preCenteredItemGeometry) {
            mesh.submitItem(poseStack, collector, light, overlay);
        } else {
            mesh.submitGroundItem(poseStack, collector, light, overlay);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        Vector3fc[] extents = preCenteredItemGeometry
                ? MoldMeshBuilder.ITEM_EXTENTS
                : MoldMeshBuilder.GROUND_ITEM_EXTENTS;
        for (Vector3fc extent : extents) {
            consumer.accept(extent);
        }
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    /** Resource-facing 26.1 special-renderer type used by minecraft:special. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(true);
        }
    }

    /** Resource-facing special-renderer type for the dropped-item context. */
    public record GroundUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<GroundUnbaked> CODEC = MapCodec.unit(new GroundUnbaked());

        @Override
        public MapCodec<GroundUnbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(false);
        }
    }
}
