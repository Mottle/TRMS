package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
