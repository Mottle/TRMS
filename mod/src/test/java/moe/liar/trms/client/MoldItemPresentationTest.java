package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MoldItemPresentationTest {
    @Test
    void handItemGeometryIsCenteredWithoutReversingThePlacedMoldFacing() {
        PoseStack poseStack = new PoseStack();
        MoldMeshBuilder.centerItemGeometry(poseStack);

        Vector3f origin = poseStack.last().pose().transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
        Vector3f positiveX = poseStack.last().pose().transformPosition(1.0F, 0.0F, 0.0F, new Vector3f());
        Vector3f positiveZ = poseStack.last().pose().transformPosition(0.0F, 0.0F, 1.0F, new Vector3f());

        assertEquals(-0.5F, origin.x());
        assertEquals(-0.0625F, origin.y());
        assertEquals(-0.5F, origin.z());
        assertEquals(0.5F, positiveX.x(), "positive X must not be reversed by a hidden 180-degree yaw");
        assertEquals(0.5F, positiveZ.z(), "positive Z must not be reversed by a hidden 180-degree yaw");
    }
}
