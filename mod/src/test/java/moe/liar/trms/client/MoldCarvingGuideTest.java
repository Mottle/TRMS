package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class MoldCarvingGuideTest {
    @Test
    void drawsEveryBoundaryOfTheFourteenByFourteenInteriorBeforeTheFirstCarve() {
        MoldCarvingGuide.Layout layout = MoldCarvingGuide.layout(MoldPattern.EMPTY);
        List<MoldCarvingGuide.Line> lines = layout.lines();

        assertEquals(196, layout.cells().size());
        assertEquals(420, lines.size());
        assertTrue(lines.stream().allMatch(line -> line.startY() > 2.0F / 16.0F));
        assertTrue(lines.stream().anyMatch(line -> line.startX() == 1.0F / 16.0F
                && line.endX() == 1.0F / 16.0F));
        assertTrue(lines.stream().anyMatch(line -> line.startX() == 15.0F / 16.0F
                && line.endX() == 15.0F / 16.0F));
        assertTrue(lines.stream().anyMatch(line -> line.startZ() == 1.0F / 16.0F
                && line.endZ() == 1.0F / 16.0F));
        assertTrue(lines.stream().anyMatch(line -> line.startZ() == 15.0F / 16.0F
                && line.endZ() == 15.0F / 16.0F));
    }

    @Test
    void drawsOnlyTheEightLegalNeighboursAfterTheFirstCarve() {
        MoldPattern pattern = MoldPattern.EMPTY.predictCarve(8, 8).orElseThrow();
        MoldCarvingGuide.Layout layout = MoldCarvingGuide.layout(pattern);

        assertEquals(8, layout.cells().size());
        assertTrue(layout.cells().stream().allMatch(cell -> pattern.canCarveAt(cell.x(), cell.z())));
        assertTrue(layout.cells().stream().noneMatch(cell -> cell.x() == 8 && cell.z() == 8));
        assertEquals(List.of(new MoldCarvingGuide.Cell(8, 8)), layout.carvedCells());
    }

    @Test
    void fillsEveryPreviouslyCarvedCellAndNoUncarvedCell() {
        MoldPattern pattern = MoldPattern.EMPTY
                .predictCarve(7, 7).orElseThrow()
                .predictCarve(8, 8).orElseThrow()
                .predictCarve(9, 8).orElseThrow();
        MoldCarvingGuide.Layout layout = MoldCarvingGuide.layout(pattern);

        assertEquals(List.of(
                new MoldCarvingGuide.Cell(7, 7),
                new MoldCarvingGuide.Cell(8, 8),
                new MoldCarvingGuide.Cell(9, 8)
        ), layout.carvedCells());
        assertTrue(layout.carvedCells().stream().allMatch(cell -> pattern.isCarved(cell.x(), cell.z())));
    }

    @Test
    void rendersEveryCarvedMarkerAsOneRaisedTranslucentVoxel() {
        MoldCarvingGuide.Cell cell = new MoldCarvingGuide.Cell(7, 9);
        MoldCarvingGuide.HighlightCube cube = cell.carvedHighlightCube(Set.of(cell));

        assertEquals(1.0F / 16.0F, cube.maxY() - cube.minY());
        assertTrue(cube.minY() > 1.0F / 16.0F);
        assertTrue(cube.maxY() > 2.0F / 16.0F,
                "the cube top must remain visible above the carved cell's rim");
        assertTrue(cube.minX() > 7.0F / 16.0F && cube.maxX() < 8.0F / 16.0F);
        assertTrue(cube.minZ() > 9.0F / 16.0F && cube.maxZ() < 10.0F / 16.0F);
    }

    @Test
    void joinsAdjacentCarvedMarkersWithoutAnInternalGapOrSideFace() {
        MoldCarvingGuide.Cell left = new MoldCarvingGuide.Cell(7, 9);
        MoldCarvingGuide.Cell right = new MoldCarvingGuide.Cell(8, 9);
        Set<MoldCarvingGuide.Cell> connectedCells = Set.of(left, right);

        MoldCarvingGuide.HighlightCube leftCube = left.carvedHighlightCube(connectedCells);
        MoldCarvingGuide.HighlightCube rightCube = right.carvedHighlightCube(connectedCells);

        assertEquals(leftCube.maxX(), rightCube.minX(),
                "connected markers must share one continuous X boundary");
        assertFalse(leftCube.eastFace(), "a connected neighbour must hide the left cube's internal east face");
        assertFalse(rightCube.westFace(), "a connected neighbour must hide the right cube's internal west face");
        assertTrue(leftCube.westFace());
        assertTrue(rightCube.eastFace());
    }

    @Test
    void suppliesEachGuideLineOwnUnitDirectionForScreenSpaceExpansion() {
        MoldCarvingGuide.Line eastWest = new MoldCarvingGuide.Line(1.0F, 2.0F, 3.0F, 5.0F, 2.0F, 3.0F);
        MoldCarvingGuide.Line northSouth = new MoldCarvingGuide.Line(1.0F, 2.0F, 3.0F, 1.0F, 2.0F, 7.0F);

        assertEquals(new MoldCarvingGuide.LineDirection(1.0F, 0.0F, 0.0F), eastWest.direction());
        assertEquals(new MoldCarvingGuide.LineDirection(0.0F, 0.0F, 1.0F), northSouth.direction());
    }

    @Test
    void mapsTheRotatedPresentationBackToServerLegalUpperInteriorCells() {
        assertEquals(new MoldCarvingGuide.Cell(11, 6),
                MoldCarvingGuide.targetCell(4.25D / 16.0D, 2.0D / 16.0D, 9.75D / 16.0D));
        assertNull(MoldCarvingGuide.targetCell(4.25D / 16.0D, 1.0D / 16.0D - 0.00001D, 9.75D / 16.0D));
        assertNull(MoldCarvingGuide.targetCell(4.25D / 16.0D, 2.0D / 16.0D + 0.00001D, 9.75D / 16.0D));
        assertNull(MoldCarvingGuide.targetCell(0.5D / 16.0D, 2.0D / 16.0D, 4.5D / 16.0D));
    }

    @Test
    void mapsEveryHorizontalPresentationBackToTheStoredPatternCoordinates() {
        double localX = 4.25D / 16.0D;
        double localZ = 9.75D / 16.0D;

        assertEquals(new MoldCarvingGuide.Cell(4, 9),
                MoldCarvingGuide.targetCell(localX, 2.0D / 16.0D, localZ, Direction.SOUTH));
        assertEquals(new MoldCarvingGuide.Cell(9, 11),
                MoldCarvingGuide.targetCell(localX, 2.0D / 16.0D, localZ, Direction.WEST));
        assertEquals(new MoldCarvingGuide.Cell(11, 6),
                MoldCarvingGuide.targetCell(localX, 2.0D / 16.0D, localZ, Direction.NORTH));
        assertEquals(new MoldCarvingGuide.Cell(6, 4),
                MoldCarvingGuide.targetCell(localX, 2.0D / 16.0D, localZ, Direction.EAST));
    }

    @Test
    void mapsEveryPresentedInteriorCellBackToItsStoredCoordinates() {
        for (Direction facing : List.of(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)) {
            for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
                for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                    MoldCarvingGuide.Cell presented = presentedCell(x, z, facing);
                    assertEquals(new MoldCarvingGuide.Cell(x, z),
                            MoldCarvingGuide.targetCell(
                                    (presented.x() + 0.5D) / 16.0D,
                                    2.0D / 16.0D,
                                    (presented.z() + 0.5D) / 16.0D,
                                    facing
                            ),
                            "round trip failed for " + facing + " at " + x + "," + z);
                }
            }
        }
    }

    @Test
    void hoverPulseOscillatesWithinTheSemiTransparentRange() {
        int midpoint = MoldCarvingGuide.hoverPulseAlpha(0.0F);
        int peak = MoldCarvingGuide.hoverPulseAlpha((float) (Math.PI / 2.0D / 0.25D));
        int trough = MoldCarvingGuide.hoverPulseAlpha((float) (Math.PI * 3.0D / 2.0D / 0.25D));

        assertTrue(peak > midpoint);
        assertTrue(trough < midpoint);
        assertTrue(trough >= 92 && peak <= 224);
    }

    private static MoldCarvingGuide.Cell presentedCell(int x, int z, Direction facing) {
        return switch (facing) {
            case SOUTH -> new MoldCarvingGuide.Cell(x, z);
            case WEST -> new MoldCarvingGuide.Cell(15 - z, x);
            case NORTH -> new MoldCarvingGuide.Cell(15 - x, 15 - z);
            case EAST -> new MoldCarvingGuide.Cell(z, 15 - x);
            case UP, DOWN -> throw new IllegalArgumentException("Mold facing must be horizontal: " + facing);
        };
    }
}
