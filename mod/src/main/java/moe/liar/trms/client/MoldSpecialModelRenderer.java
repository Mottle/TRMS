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
    private final Presentation presentation;
    private final boolean clay;

    /** Creates the standard third-person, fixed, and fallback item renderer. */
    public MoldSpecialModelRenderer() {
        this(Presentation.STANDARD, false);
    }

    private MoldSpecialModelRenderer(Presentation presentation, boolean clay) {
        this.presentation = presentation;
        this.clay = clay;
    }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int seed) {
        MoldPattern pattern = stack.getOrDefault(TrmsClientMod.MOLD_PATTERN.get(), MoldPattern.EMPTY);
        MoldMeshBuilder.Mesh mesh = meshCache.get(pattern, 0L,
                clay ? MoldMeshBuilder.currentClaySprite() : MoldMeshBuilder.currentTerracottaSprite());
        switch (presentation) {
            case STANDARD -> mesh.submitItem(poseStack, collector, light, overlay);
            case FIRST_PERSON -> mesh.submitFirstPersonItem(poseStack, collector, light, overlay);
            case GROUND -> mesh.submitGroundItem(poseStack, collector, light, overlay);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        Vector3fc[] extents = switch (presentation) {
            case STANDARD -> MoldMeshBuilder.ITEM_EXTENTS;
            case FIRST_PERSON -> MoldMeshBuilder.ITEM_EXTENTS;
            case GROUND -> MoldMeshBuilder.GROUND_ITEM_EXTENTS;
        };
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
            return new MoldSpecialModelRenderer(Presentation.STANDARD, false);
        }
    }

    /** Resource-facing renderer retaining the mold's established first-person coordinates. */
    public record FirstPersonUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<FirstPersonUnbaked> CODEC = MapCodec.unit(new FirstPersonUnbaked());

        @Override
        public MapCodec<FirstPersonUnbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(Presentation.FIRST_PERSON, false);
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
            return new MoldSpecialModelRenderer(Presentation.GROUND, false);
        }
    }

    public record BlankUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<BlankUnbaked> CODEC = MapCodec.unit(new BlankUnbaked());

        @Override
        public MapCodec<BlankUnbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(Presentation.STANDARD, true);
        }
    }

    public record BlankFirstPersonUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<BlankFirstPersonUnbaked> CODEC = MapCodec.unit(new BlankFirstPersonUnbaked());

        @Override
        public MapCodec<BlankFirstPersonUnbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(Presentation.FIRST_PERSON, true);
        }
    }

    public record BlankGroundUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<BlankGroundUnbaked> CODEC = MapCodec.unit(new BlankGroundUnbaked());

        @Override
        public MapCodec<BlankGroundUnbaked> type() {
            return CODEC;
        }

        @Override
        public MoldSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MoldSpecialModelRenderer(Presentation.GROUND, true);
        }
    }

    private enum Presentation {
        STANDARD,
        FIRST_PERSON,
        GROUND
    }
}
