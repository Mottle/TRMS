package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

/** Renders the exact closed silhouette recorded in a completed weapon-part item component. */
public final class WeaponPartSpecialModelRenderer implements SpecialModelRenderer<ItemStack> {
    private final MoldMeshBuilder.Cache meshCache = new MoldMeshBuilder.Cache(false);
    private final Presentation presentation;

    public WeaponPartSpecialModelRenderer() {
        this(Presentation.STANDARD);
    }

    private WeaponPartSpecialModelRenderer(Presentation presentation) {
        this.presentation = presentation;
    }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int seed) {
        WeaponPartData part = stack.get(TrmsClientMod.WEAPON_PART.get());
        if (part == null) {
            return;
        }
        MoldFillVisual visual = MoldFillVisual.forMaterial(part.material());
        if (visual == null) {
            return;
        }
        MoldMeshBuilder.Mesh mesh = meshCache.getWeaponPart(part.pattern(), MoldMeshBuilder.currentSolidMetalSprite());
        switch (presentation) {
            case GUI -> mesh.submitTintedWeaponPartItem(poseStack, collector, light, overlay,
                    part.pattern(), true, visual.baseColor());
            case FIRST_PERSON -> mesh.submitTintedFirstPersonWeaponPartItem(poseStack, collector, light, overlay,
                    part.pattern(), visual.baseColor());
            case STANDARD -> mesh.submitTintedWeaponPartItem(poseStack, collector, light, overlay,
                    part.pattern(), false, visual.baseColor());
            case GROUND -> mesh.submitTintedGroundItem(poseStack, collector, light, overlay, visual.baseColor());
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        Vector3fc[] extents = switch (presentation) {
            case GUI -> MoldMeshBuilder.WEAPON_PART_GUI_EXTENTS;
            case FIRST_PERSON -> MoldMeshBuilder.WEAPON_PART_FIRST_PERSON_EXTENTS;
            case STANDARD -> MoldMeshBuilder.WEAPON_PART_ITEM_EXTENTS;
            case GROUND -> MoldMeshBuilder.WEAPON_PART_GROUND_ITEM_EXTENTS;
        };
        for (Vector3fc extent : extents) {
            consumer.accept(extent);
        }
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    /** Resource-facing renderer used by hand, fixed, and other non-GUI item contexts. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public WeaponPartSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new WeaponPartSpecialModelRenderer(Presentation.STANDARD);
        }
    }

    /** Resource-facing renderer that expands a casting only in inventory-style GUI contexts. */
    public record GuiUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<GuiUnbaked> CODEC = MapCodec.unit(new GuiUnbaked());

        @Override
        public MapCodec<GuiUnbaked> type() {
            return CODEC;
        }

        @Override
        public WeaponPartSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new WeaponPartSpecialModelRenderer(Presentation.GUI);
        }
    }

    /** Resource-facing renderer that maps mold-surface coordinates to the native handheld item plane. */
    public record FirstPersonUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<FirstPersonUnbaked> CODEC = MapCodec.unit(new FirstPersonUnbaked());

        @Override
        public MapCodec<FirstPersonUnbaked> type() {
            return CODEC;
        }

        @Override
        public WeaponPartSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new WeaponPartSpecialModelRenderer(Presentation.FIRST_PERSON);
        }
    }

    /** Resource-facing renderer for dropped item entities' raw ground model coordinates. */
    public record GroundUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<GroundUnbaked> CODEC = MapCodec.unit(new GroundUnbaked());

        @Override
        public MapCodec<GroundUnbaked> type() {
            return CODEC;
        }

        @Override
        public WeaponPartSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new WeaponPartSpecialModelRenderer(Presentation.GROUND);
        }
    }

    private enum Presentation {
        GUI,
        FIRST_PERSON,
        STANDARD,
        GROUND
    }
}
