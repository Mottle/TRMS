package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** Pure topology tests: no Minecraft client, atlas, sprite, or GUI is required. */
class MoldMeshTopologyTest {
    @Test
    void droppedItemExtentsRemainInRawBlockModelSpace() {
        assertEquals(0.0f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[0].x());
        assertEquals(0.0f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[0].y());
        assertEquals(0.0f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[0].z());
        assertEquals(1.0f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[1].x());
        assertEquals(0.125f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[1].y());
        assertEquals(1.0f, MoldMeshBuilder.GROUND_ITEM_EXTENTS[1].z());
    }

    @Test
    void emptyWorldMeshContainsOneTopQuadPerSolidInteriorCell() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(MoldPattern.EMPTY, false);
        assertEquals(196, quads.size());
        assertEquals(196, countTopFaces(quads));
        assertEquals(0, countVerticalFaces(quads));
    }

    @Test
    void firstHoleRemovesOneTopAndAddsItsFourInteriorWalls() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(carve(4, 4), false);
        assertEquals(199, quads.size());
        assertEquals(195, countTopFaces(quads));
        assertEquals(4, countVerticalFaces(quads));
    }

    @Test
    void fillTopologyIsEmptyUntilTheMoldHasACarvedCell() {
        assertTrue(MoldMeshTopology.buildFill(MoldPattern.EMPTY).isEmpty());
    }

    @Test
    void fillTopologyCoversOneCarvedCellWithOneTopAndFourFlowingSides() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.buildFill(carve(4, 4));

        assertEquals(5, quads.size());
        assertEquals(1, countTopFaces(quads));
        assertEquals(4, countVerticalFaces(quads));
        assertTrue(quads.stream().filter(quad -> quad.ny() > 0.0F)
                .allMatch(quad -> minY(quad) == MoldMeshTopology.FILL_SURFACE_Y
                        && maxY(quad) == MoldMeshTopology.FILL_SURFACE_Y));
        assertTrue(quads.stream().filter(quad -> quad.ny() == 0.0F)
                .allMatch(quad -> minY(quad) == MoldMeshTopology.FILL_BASE_Y
                        && maxY(quad) == MoldMeshTopology.FILL_SURFACE_Y));
    }

    @Test
    void fillSurfaceLeavesVisibleHeadroomBelowTheCeramicRim() {
        assertEquals(1.75F, MoldMeshTopology.FILL_SURFACE_Y);
        assertTrue(MoldMeshTopology.FILL_SURFACE_Y < 2.0F);
        assertEquals(0.25F, 2.0F - MoldMeshTopology.FILL_SURFACE_Y);
    }

    @Test
    void fillSideWallsAreInsetFromTheSolidCeramicWalls() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.buildFill(carve(4, 4));

        assertTrue(quads.stream().filter(quad -> quad.nx() < 0.0F)
                .allMatch(quad -> quad.x0() > 4.0F));
        assertTrue(quads.stream().filter(quad -> quad.nx() > 0.0F)
                .allMatch(quad -> quad.x0() < 5.0F));
        assertTrue(quads.stream().filter(quad -> quad.nz() < 0.0F)
                .allMatch(quad -> quad.z0() > 4.0F));
        assertTrue(quads.stream().filter(quad -> quad.nz() > 0.0F)
                .allMatch(quad -> quad.z0() < 5.0F));
    }

    @Test
    void adjacentFillCellsRemoveTheirSharedInternalSide() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.buildFill(carve(4, 4, 5, 4));

        assertEquals(8, quads.size());
        assertEquals(2, countTopFaces(quads));
        assertEquals(6, countVerticalFaces(quads));
        assertFalse(quads.stream().anyMatch(quad -> quad.ny() == 0.0F
                && quad.x0() == 5.0F && quad.x1() == 5.0F
                && quad.x2() == 5.0F && quad.x3() == 5.0F
                && minZ(quad) == 4.0F && maxZ(quad) == 5.0F));
    }

    @Test
    void weaponPartTopologyIsClosedAndRetainsOnlyOuterSides() {
        List<MoldMeshTopology.Quad> oneCell = MoldMeshTopology.buildWeaponPart(carve(4, 4));
        List<MoldMeshTopology.Quad> twoCells = MoldMeshTopology.buildWeaponPart(carve(4, 4, 5, 4));

        assertEquals(6, oneCell.size());
        assertEquals(1, oneCell.stream().filter(quad -> quad.ny() == 1).count());
        assertEquals(1, oneCell.stream().filter(quad -> quad.ny() == -1).count());
        assertEquals(10, twoCells.size());
        assertFalse(twoCells.stream().anyMatch(quad -> quad.nx() != 0.0F
                && quad.x0() == 5.0F && quad.x1() == 5.0F
                && quad.x2() == 5.0F && quad.x3() == 5.0F
                && minZ(quad) == 4.0F && maxZ(quad) == 5.0F));
    }

    @Test
    void orthogonallyAdjacentHolesDoNotLeaveASharedInteriorWall() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(carve(4, 4, 5, 4), false);
        assertEquals(200, quads.size());
        assertEquals(194, countTopFaces(quads));
        assertEquals(6, countVerticalFaces(quads));
        assertFalse(quads.stream().anyMatch(quad -> quad.nx() != 0
                && quad.x0() == 5 && quad.x1() == 5 && quad.x2() == 5 && quad.x3() == 5
                && Math.min(quad.z0(), quad.z1()) == 4));
    }

    @Test
    void diagonalHolesDoNotCancelAStraightNeighbourWall() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(carve(4, 4, 5, 5), false);
        assertEquals(202, quads.size());
        assertEquals(194, countTopFaces(quads));
        assertEquals(8, countVerticalFaces(quads));
    }

    @Test
    void borderAdjacentHoleGetsTheRimToHoleInnerWall() {
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(carve(1, 5), true);
        // Complete-shell rendering includes the fixed outer wall quads as well
        // as the 60 rim-cell tops and the dynamic inner wall.
        assertEquals(329, quads.size());
        assertTrue(quads.stream().anyMatch(quad -> quad.nx() == 1
                && quad.x0() == 1 && quad.x1() == 1 && quad.x2() == 1 && quad.x3() == 1
                && minZ(quad) == 5
                && maxZ(quad) == 6));
    }

    @Test
    void everyGeneratedVertexCarriesUvOverlayAndLightAttributes() {
        int overlay = 0x12345678;
        int light = 0x00F000F0;
        for (boolean completeShell : new boolean[] {false, true}) {
            for (MoldMeshTopology.Quad quad : MoldMeshTopology.build(carve(4, 4), completeShell)) {
                for (int index = 0; index < 4; index++) {
                    MoldMeshTopology.Vertex vertex = quad.vertex(index, overlay, light);
                    assertEquals(overlay, vertex.overlay());
                    assertEquals(light, vertex.light());
                    assertTrue(vertex.u() >= 0.0f && vertex.u() <= 1.0f);
                    assertTrue(vertex.v() >= 0.0f && vertex.v() <= 1.0f);
                }
            }
        }
    }

    @Test
    void dynamicTextureCoordinatesAreContinuousAcrossTheWholeMold() {
        MoldMeshTopology.TextureCoordinates topNearCorner =
                MoldMeshTopology.textureCoordinates(1, 2, 1, 0, 1, 0);
        MoldMeshTopology.TextureCoordinates topSharedBoundary =
                MoldMeshTopology.textureCoordinates(2, 2, 1, 0, 1, 0);
        MoldMeshTopology.TextureCoordinates topFarCorner =
                MoldMeshTopology.textureCoordinates(15, 2, 15, 0, 1, 0);
        MoldMeshTopology.TextureCoordinates westWall =
                MoldMeshTopology.textureCoordinates(4, 1, 5, -1, 0, 0);

        assertEquals(1.0f / 16.0f, topNearCorner.u());
        assertEquals(1.0f / 16.0f, topNearCorner.v());
        assertEquals(2.0f / 16.0f, topSharedBoundary.u());
        assertEquals(15.0f / 16.0f, topFarCorner.u());
        assertEquals(15.0f / 16.0f, topFarCorner.v());
        assertEquals(5.0f / 16.0f, westWall.u());
        assertEquals(0.5f, westWall.v());
    }

    @Test
    void generatedQuadsUseTheCanonicalFaceInfoVertexOrder() {
        MoldMeshTopology.Quad top = MoldMeshTopology.build(MoldPattern.EMPTY, false).getFirst();
        assertEquals(1.0f, top.x0());
        assertEquals(2.0f, top.y0());
        assertEquals(1.0f, top.z0());
        assertEquals(1.0f, top.x1());
        assertEquals(2.0f, top.y1());
        assertEquals(2.0f, top.z1());
        assertEquals(2.0f, top.x2());
        assertEquals(2.0f, top.y2());
        assertEquals(2.0f, top.z2());
        assertEquals(2.0f, top.x3());
        assertEquals(2.0f, top.y3());
        assertEquals(1.0f, top.z3());
    }

    @Test
    void presentationRotationTurnsGeometryWithoutChangingQuadWinding() {
        MoldMeshTopology.Quad source = MoldMeshTopology.build(MoldPattern.EMPTY, false).getFirst();
        MoldMeshTopology.Quad rotated = MoldMeshTopology.rotateForPresentation(source);

        assertEquals(15.0f, rotated.x0());
        assertEquals(15.0f, rotated.z0());
        assertEquals(14.0f, rotated.x2());
        assertEquals(14.0f, rotated.z2());
        assertEquals(source.ny(), rotated.ny());
        assertEquals(-source.nx(), rotated.nx());
        assertEquals(-source.nz(), rotated.nz());
    }

    @Test
    void horizontalPresentationTurnsMatchTheBlockFacingConvention() {
        MoldMeshTopology.Quad source = MoldMeshTopology.build(MoldPattern.EMPTY, false).getFirst();
        MoldMeshTopology.Quad south = MoldMeshTopology.rotateForPresentation(source, Direction.SOUTH);
        MoldMeshTopology.Quad west = MoldMeshTopology.rotateForPresentation(source, Direction.WEST);
        MoldMeshTopology.Quad east = MoldMeshTopology.rotateForPresentation(source, Direction.EAST);

        assertEquals(source, south);
        assertEquals(15.0f, west.x0());
        assertEquals(1.0f, west.z0());
        assertEquals(2.0f, west.z2());
        assertEquals(1.0f, east.x0());
        assertEquals(15.0f, east.z0());
        assertEquals(14.0f, east.z2());
    }

    @Test
    void everyFaceWindingMatchesItsOutwardNormal() {
        for (MoldPattern pattern : List.of(MoldPattern.EMPTY, carve(4, 4), carve(1, 5))) {
            for (boolean completeShell : new boolean[] {false, true}) {
                for (MoldMeshTopology.Quad quad : MoldMeshTopology.build(pattern, completeShell)) {
                    float ax = quad.x1() - quad.x0();
                    float ay = quad.y1() - quad.y0();
                    float az = quad.z1() - quad.z0();
                    float bx = quad.x2() - quad.x0();
                    float by = quad.y2() - quad.y0();
                    float bz = quad.z2() - quad.z0();
                    float crossX = ay * bz - az * by;
                    float crossY = az * bx - ax * bz;
                    float crossZ = ax * by - ay * bx;

                    float alignment = crossX * quad.nx()
                            + crossY * quad.ny()
                            + crossZ * quad.nz();
                    assertTrue(alignment > 0.0f,
                            () -> "face winding disagrees with normal: " + quad);
                }
            }
        }
    }

    @Test
    void topFacesPointUpAndHoleWallsPointIntoTheCarvedCell() {
        MoldPattern pattern = carve(4, 4);
        List<MoldMeshTopology.Quad> quads = MoldMeshTopology.build(pattern, false);

        assertTrue(quads.stream().filter(quad -> quad.ny() == 1)
                .allMatch(quad -> quad.y0() == 2 && quad.y1() == 2
                        && quad.y2() == 2 && quad.y3() == 2));
        assertTrue(quads.stream().filter(quad -> quad.ny() == 0)
                .allMatch(quad -> minY(quad) == 1 && maxY(quad) == 2));
        assertTrue(quads.stream().anyMatch(quad -> quad.nx() == 1
                && quad.x0() == 4 && quad.x1() == 4 && quad.x2() == 4 && quad.x3() == 4));
        assertTrue(quads.stream().anyMatch(quad -> quad.nx() == -1
                && quad.x0() == 5 && quad.x1() == 5 && quad.x2() == 5 && quad.x3() == 5));
        assertTrue(quads.stream().anyMatch(quad -> quad.nz() == 1
                && quad.z0() == 4 && quad.z1() == 4 && quad.z2() == 4 && quad.z3() == 4));
        assertTrue(quads.stream().anyMatch(quad -> quad.nz() == -1
                && quad.z0() == 5 && quad.z1() == 5 && quad.z2() == 5 && quad.z3() == 5));
    }

    private static MoldPattern carve(int... coordinates) {
        MoldPattern pattern = MoldPattern.EMPTY;
        for (int index = 0; index < coordinates.length; index += 2) {
            pattern = pattern.predictCarve(coordinates[index], coordinates[index + 1]).orElseThrow();
        }
        return pattern;
    }

    private static long countTopFaces(List<MoldMeshTopology.Quad> quads) {
        return quads.stream().filter(quad -> quad.ny() == 1).count();
    }

    private static long countVerticalFaces(List<MoldMeshTopology.Quad> quads) {
        return quads.stream().filter(quad -> quad.ny() == 0).count();
    }

    private static float minY(MoldMeshTopology.Quad quad) {
        return Math.min(Math.min(quad.y0(), quad.y1()), Math.min(quad.y2(), quad.y3()));
    }

    private static float maxY(MoldMeshTopology.Quad quad) {
        return Math.max(Math.max(quad.y0(), quad.y1()), Math.max(quad.y2(), quad.y3()));
    }

    private static float minZ(MoldMeshTopology.Quad quad) {
        return Math.min(Math.min(quad.z0(), quad.z1()), Math.min(quad.z2(), quad.z3()));
    }

    private static float maxZ(MoldMeshTopology.Quad quad) {
        return Math.max(Math.max(quad.z0(), quad.z1()), Math.max(quad.z2(), quad.z3()));
    }
}
