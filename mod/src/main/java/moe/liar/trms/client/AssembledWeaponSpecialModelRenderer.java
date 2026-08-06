package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Renders the casting and its pixelated wooden handle from the item component. */
public final class AssembledWeaponSpecialModelRenderer implements SpecialModelRenderer<ItemStack> {
    private final Presentation presentation;
    private final MoldMeshBuilder.Cache castingCache = new MoldMeshBuilder.Cache(false);
    private net.minecraft.client.renderer.texture.TextureAtlasSprite handleSprite;
    private final Map<Long, MoldMeshBuilder.Mesh> handleCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, MoldMeshBuilder.Mesh> eldest) {
            return size() > 64;
        }
    };

    private AssembledWeaponSpecialModelRenderer() {
        this(Presentation.STANDARD);
    }

    private AssembledWeaponSpecialModelRenderer(Presentation presentation) {
        this.presentation = presentation;
    }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int seed) {
        AssembledWeaponData data = stack.get(TrmsClientMod.ASSEMBLED_WEAPON.get());
        if (data == null) return;
        MoldFillVisual visual = MoldFillVisual.forMaterial(data.material());
        if (visual == null) return;
        MoldMeshBuilder.Mesh casting = castingCache.getWeaponPart(data.pattern(), MoldMeshBuilder.currentSolidMetalSprite());
        var woodSprite = MoldMeshBuilder.currentWoodSprite();
        if (woodSprite != handleSprite) {
            handleSprite = woodSprite;
            handleCache.clear();
        }
        long handleKey = ((long) data.connectionX() << 32) ^ (data.connectionZ() & 0xFFFFFFFFL);
        MoldMeshBuilder.Mesh handle = handleCache.computeIfAbsent(handleKey,
                ignored -> MoldMeshBuilder.buildHandle(data.connectionX(), data.connectionZ(), woodSprite));
        poseStack.pushPose();
        switch (presentation) {
            case GUI -> MoldMeshBuilder.centerAssembledWeaponGeometry(poseStack, data.pattern(),
                    data.connectionX(), data.connectionZ(), true);
            case FIRST_PERSON -> {
                MoldMeshBuilder.centerFirstPersonAssembledWeaponGeometry(poseStack, data.pattern(),
                        data.connectionX(), data.connectionZ());
            }
            case FIXED -> MoldMeshBuilder.centerFixedAssembledWeaponGeometry(poseStack, data.pattern(),
                    data.connectionX(), data.connectionZ());
            case GROUND -> MoldMeshBuilder.centerGroundAssembledWeaponGeometry(poseStack, data.pattern(),
                    data.connectionX(), data.connectionZ());
            case THIRD_PERSON -> MoldMeshBuilder.centerThirdPersonAssembledWeaponGeometry(poseStack, data.pattern(),
                    data.connectionX(), data.connectionZ());
            case STANDARD -> MoldMeshBuilder.centerAssembledWeaponGeometry(poseStack, data.pattern(),
                    data.connectionX(), data.connectionZ(), false);
        }
        casting.submitTintedItem(poseStack, collector, light, overlay, visual.baseColor());
        handle.submitWoodItem(poseStack, collector, light, overlay);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        // The canonical handle may extend to z=23/16 before the item transform;
        // provide conservative bounds so the special renderer is never culled.
        consumer.accept(new Vector3f(-1.0F, -1.0F, -1.0F));
        consumer.accept(new Vector3f(2.0F, 2.0F, 2.0F));
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) { return stack; }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer();
        }
    }

    public record GuiUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<GuiUnbaked> CODEC = MapCodec.unit(new GuiUnbaked());
        @Override public MapCodec<GuiUnbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer(Presentation.GUI);
        }
    }

    public record FirstPersonUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<FirstPersonUnbaked> CODEC = MapCodec.unit(new FirstPersonUnbaked());
        @Override public MapCodec<FirstPersonUnbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer(Presentation.FIRST_PERSON);
        }
    }

    public record ThirdPersonUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<ThirdPersonUnbaked> CODEC = MapCodec.unit(new ThirdPersonUnbaked());
        @Override public MapCodec<ThirdPersonUnbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer(Presentation.THIRD_PERSON);
        }
    }

    public record FixedUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<FixedUnbaked> CODEC = MapCodec.unit(new FixedUnbaked());
        @Override public MapCodec<FixedUnbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer(Presentation.FIXED);
        }
    }

    public record GroundUnbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<GroundUnbaked> CODEC = MapCodec.unit(new GroundUnbaked());
        @Override public MapCodec<GroundUnbaked> type() { return CODEC; }
        @Override public AssembledWeaponSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AssembledWeaponSpecialModelRenderer(Presentation.GROUND);
        }
    }

    private enum Presentation { GUI, FIRST_PERSON, THIRD_PERSON, FIXED, GROUND, STANDARD }
}
