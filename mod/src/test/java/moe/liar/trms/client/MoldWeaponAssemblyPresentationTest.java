package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MoldWeaponAssemblyPresentationTest {
    @Test
    void handleUsesOneByOneByTenPixelGeometry() {
        var quads = MoldMeshTopology.buildHandle(6, 7);
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (MoldMeshTopology.Quad quad : quads) {
            float[] xs = {quad.x0(), quad.x1(), quad.x2(), quad.x3()};
            float[] ys = {quad.y0(), quad.y1(), quad.y2(), quad.y3()};
            float[] zs = {quad.z0(), quad.z1(), quad.z2(), quad.z3()};
            for (int i = 0; i < 4; i++) {
                minX = Math.min(minX, xs[i]); maxX = Math.max(maxX, xs[i]);
                minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]);
                minZ = Math.min(minZ, zs[i]); maxZ = Math.max(maxZ, zs[i]);
            }
        }
        assertEquals(1.0F, maxX - minX);
        assertEquals(1.0F, maxY - minY);
        assertEquals(10.0F, maxZ - minZ);
    }

    @Test
    void combinedPresentationIncludesTheEntireCastingAndHandleBounds() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow();

        MoldMeshBuilder.AssembledWeaponItemPresentation standard =
                MoldMeshBuilder.assembledWeaponItemPresentation(pattern, 6, 7, false);
        MoldMeshBuilder.AssembledWeaponItemPresentation gui =
                MoldMeshBuilder.assembledWeaponItemPresentation(pattern, 6, 7, true);

        assertEquals(13.0F / 32.0F, standard.centerX());
        assertEquals(23.0F / 32.0F, standard.centerZ());
        assertEquals(1.0F, standard.uniformScale());
        assertEquals(14.0F / 11.0F, gui.uniformScale());
    }

    @Test
    void firstPersonAssemblyIsMappedToTheHandheldPlaneAndLoweredTwoPixels() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerFirstPersonAssembledWeaponGeometry(poseStack, pattern, 6, 7);

        Vector3f center = poseStack.last().pose().transformPosition(
                13.0F / 32.0F, 0.0F, 23.0F / 32.0F, new Vector3f());
        assertEquals(0.0F, center.x(), 0.000001F);
        assertEquals(-4.0F / 32.0F, center.y(), 0.000001F);
        assertEquals(-1.0F / 32.0F, center.z(), 0.000001F);
    }

    @Test
    void thirdPersonAssemblyQuarterTurnTurnsTheHandleAxis() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerThirdPersonAssembledWeaponGeometry(poseStack, pattern, 6, 7);

        Vector3f first = poseStack.last().pose().transformPosition(0.25F, 0.0F, 0.50F, new Vector3f());
        Vector3f second = poseStack.last().pose().transformPosition(0.25F, 0.0F, 0.75F, new Vector3f());
        Vector3f delta = second.sub(first);
        assertEquals(0.25F, Math.abs(delta.x()), 0.000001F,
                "the handle's former Z axis must become the horizontal third-person axis");
        assertEquals(0.0F, delta.z(), 0.000001F);
    }
}
