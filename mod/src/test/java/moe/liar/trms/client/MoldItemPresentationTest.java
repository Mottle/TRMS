package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MoldItemPresentationTest {
    @Test
    void standardMoldItemExtentsStayInRawBlockModelSpace() {
        assertEquals(0.0F, MoldMeshBuilder.ITEM_EXTENTS[0].x());
        assertEquals(0.0F, MoldMeshBuilder.ITEM_EXTENTS[0].y());
        assertEquals(0.0F, MoldMeshBuilder.ITEM_EXTENTS[0].z());
        assertEquals(1.0F, MoldMeshBuilder.ITEM_EXTENTS[1].x());
        assertEquals(0.125F, MoldMeshBuilder.ITEM_EXTENTS[1].y());
        assertEquals(1.0F, MoldMeshBuilder.ITEM_EXTENTS[1].z());
    }

    @Test
    void firstPersonMoldUsesTheStandardItemPivotOnly() {
        PoseStack poseStack = new PoseStack();
        // ItemStackRenderState applies this pivot before a special renderer.
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Vector3f moldCenter = poseStack.last().pose().transformPosition(0.5F, 0.0625F, 0.5F, new Vector3f());

        assertEquals(0.0F, moldCenter.x());
        assertEquals(-0.4375F, moldCenter.y());
        assertEquals(0.0F, moldCenter.z());
    }

    @Test
    void weaponPartItemPresentationCentersAndExpandsASingleCarvedCell() {
        MoldPattern pattern = carve(4, 6);
        MoldMeshBuilder.WeaponPartItemPresentation presentation =
                MoldMeshBuilder.weaponPartItemPresentation(pattern, true);
        assertEquals(4.5F / 16.0F, presentation.centerX());
        assertEquals(6.5F / 16.0F, presentation.centerZ());
        assertEquals(14.0F, presentation.uniformScale());

        PoseStack poseStack = new PoseStack();
        // ItemStackRenderState applies this standard item-model centering
        // before invoking a special renderer. Reproduce it here so this test
        // covers the exact coordinate space used by the inventory GUI.
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerWeaponPartItemGeometry(poseStack, pattern, true);
        Vector3f minimum = poseStack.last().pose().transformPosition(4.0F / 16.0F, 0.0F,
                6.0F / 16.0F, new Vector3f());
        Vector3f maximum = poseStack.last().pose().transformPosition(5.0F / 16.0F, 1.0F / 16.0F,
                7.0F / 16.0F, new Vector3f());

        assertEquals(-7.0F / 16.0F, minimum.x());
        assertEquals(-7.0F / 16.0F, minimum.y());
        assertEquals(-7.0F / 16.0F, minimum.z());
        assertEquals(7.0F / 16.0F, maximum.x());
        assertEquals(7.0F / 16.0F, maximum.y());
        assertEquals(7.0F / 16.0F, maximum.z());
    }

    @Test
    void weaponPartItemPresentationPreservesAWideOutlineAspectRatio() {
        MoldPattern pattern = carve(3, 4, 4, 4, 5, 4, 6, 4, 7, 4, 8, 4, 9, 4);
        MoldMeshBuilder.WeaponPartItemPresentation presentation =
                MoldMeshBuilder.weaponPartItemPresentation(pattern, true);

        assertEquals(6.5F / 16.0F, presentation.centerX());
        assertEquals(4.5F / 16.0F, presentation.centerZ());
        assertEquals(2.0F, presentation.uniformScale());
    }

    @Test
    void heldWeaponPartCentersItsOutlineWithoutTheInventoryScaleUp() {
        MoldPattern pattern = carve(4, 6);
        MoldMeshBuilder.WeaponPartItemPresentation presentation =
                MoldMeshBuilder.weaponPartItemPresentation(pattern, false);
        assertEquals(1.0F, presentation.uniformScale());

        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerWeaponPartItemGeometry(poseStack, pattern, false);
        Vector3f minimum = poseStack.last().pose().transformPosition(4.0F / 16.0F, 0.0F,
                6.0F / 16.0F, new Vector3f());
        Vector3f maximum = poseStack.last().pose().transformPosition(5.0F / 16.0F, 1.0F / 16.0F,
                7.0F / 16.0F, new Vector3f());

        assertEquals(-1.0F / 32.0F, minimum.x());
        assertEquals(-1.0F / 32.0F, minimum.y());
        assertEquals(-1.0F / 32.0F, minimum.z());
        assertEquals(1.0F / 32.0F, maximum.x());
        assertEquals(1.0F / 32.0F, maximum.y());
        assertEquals(1.0F / 32.0F, maximum.z());
    }

    @Test
    void thirdPersonWeaponPartKeepsItsLocalGeometryIndependentOfPlayerRelativeDisplayTranslation() {
        MoldPattern pattern = carve(4, 6);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerThirdPersonWeaponPartGeometry(poseStack, pattern);

        Vector3f minimum = poseStack.last().pose().transformPosition(4.0F / 16.0F, 0.0F,
                6.0F / 16.0F, new Vector3f());
        Vector3f maximum = poseStack.last().pose().transformPosition(5.0F / 16.0F, 1.0F / 16.0F,
                7.0F / 16.0F, new Vector3f());

        assertEquals(-11.0F / 32.0F, minimum.y());
        assertEquals(-9.0F / 32.0F, maximum.y());
        assertEquals(-5.0F / 16.0F, (minimum.y() + maximum.y()) / 2.0F);
        assertEquals(-1.0F / 32.0F, minimum.z());
        assertEquals(1.0F / 32.0F, maximum.z());
    }

    @Test
    void firstPersonWeaponPartMapsTheMoldSurfaceToTheNativeItemPlane() {
        MoldPattern pattern = carve(4, 6);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerFirstPersonWeaponPartGeometry(poseStack, pattern);

        Vector3f lowerFront = poseStack.last().pose().transformPosition(4.0F / 16.0F, 0.0F,
                6.0F / 16.0F, new Vector3f());
        Vector3f upperBack = poseStack.last().pose().transformPosition(5.0F / 16.0F, 1.0F / 16.0F,
                7.0F / 16.0F, new Vector3f());

        assertEquals(-1.0F / 32.0F, lowerFront.x(), 0.000001F);
        assertEquals(1.0F / 32.0F, lowerFront.y(), 0.000001F,
                "mold Z must become native item Y with the front face preserved");
        assertEquals(-1.0F / 32.0F, lowerFront.z(), 0.000001F);
        assertEquals(1.0F / 32.0F, upperBack.x(), 0.000001F);
        assertEquals(-1.0F / 32.0F, upperBack.y(), 0.000001F);
        assertEquals(1.0F / 32.0F, upperBack.z(), 0.000001F,
                "mold thickness must become positive native item depth");
    }

    @Test
    void fixedWeaponPartUsesTheSameFlatNativeItemPlaneAsAnItemFrame() {
        MoldPattern pattern = carve(4, 6);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MoldMeshBuilder.centerFixedWeaponPartGeometry(poseStack, pattern);

        Vector3f lowerFront = poseStack.last().pose().transformPosition(4.0F / 16.0F, 0.0F,
                6.0F / 16.0F, new Vector3f());
        Vector3f upperBack = poseStack.last().pose().transformPosition(5.0F / 16.0F, 1.0F / 16.0F,
                7.0F / 16.0F, new Vector3f());

        assertEquals(-1.0F / 32.0F, lowerFront.x(), 0.000001F);
        assertEquals(1.0F / 32.0F, lowerFront.y(), 0.000001F);
        assertEquals(-1.0F / 32.0F, lowerFront.z(), 0.000001F);
        assertEquals(1.0F / 32.0F, upperBack.x(), 0.000001F);
        assertEquals(-1.0F / 32.0F, upperBack.y(), 0.000001F);
        assertEquals(1.0F / 32.0F, upperBack.z(), 0.000001F);
    }

    private static MoldPattern carve(int... coordinates) {
        MoldPattern result = MoldPattern.EMPTY;
        for (int index = 0; index < coordinates.length; index += 2) {
            result = result.predictCarve(coordinates[index], coordinates[index + 1]).orElseThrow();
        }
        return result;
    }
}
