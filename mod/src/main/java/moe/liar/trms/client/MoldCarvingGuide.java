package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Client-only overlay marking the legal next carving cells of an aimed mold.
 *
 * <p>The lines deliberately sit just above the upper voxel layer. They are
 * submitted through the block-entity renderer, so they inherit the mold's
 * world transform and never exist in inventory, item-frame, or dropped-item
 * rendering.</p>
 */
final class MoldCarvingGuide {
    private static final float CELL = 1.0F / 16.0F;
    private static final float SURFACE_Y = 2.0F / 16.0F + 0.001F;
    private static final float CARVED_CUBE_BASE_Y = 1.0F / 16.0F + 0.001F;
    private static final float CARVED_CUBE_INSET = 0.0015F;
    private static final float HOVER_FILL_Y = 2.0F / 16.0F + 0.0005F;
    private static final float HOVER_FILL_INSET = 0.003F;
    private static final float LINE_WIDTH = 5.0F;
    private static final float FRONT_ARROW_BASE_MIN_X = 6.0F / 16.0F;
    private static final float FRONT_ARROW_BASE_MAX_X = 10.0F / 16.0F;
    private static final float FRONT_ARROW_BASE_Z = 1.0F + 1.0F / 16.0F;
    private static final float FRONT_ARROW_TIP_Z = 1.0F + 3.0F / 16.0F;
    private static final float FRONT_ARROW_Y = 2.0F / 16.0F + 0.002F;
    private static final int FRONT_ARROW_ALPHA = 128;
    private static final FrontArrow FRONT_ARROW = new FrontArrow(
            FRONT_ARROW_BASE_MIN_X, FRONT_ARROW_Y, FRONT_ARROW_BASE_Z,
            FRONT_ARROW_BASE_MAX_X, FRONT_ARROW_Y, FRONT_ARROW_BASE_Z,
            8.0F / 16.0F, FRONT_ARROW_Y, FRONT_ARROW_TIP_Z);
    private MoldCarvingGuide() {
    }

    static void submit(PoseStack poseStack, SubmitNodeCollector collector, Layout layout,
                       @Nullable Cell hoveredCell, int hoveredAlpha) {
        if (!layout.carvedCells().isEmpty()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                    layout::submitCarvedFills);
        }
        if (hoveredCell != null) {
            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                    (pose, vertices) -> hoveredCell.submitHoverFill(pose, vertices, hoveredAlpha));
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(),
                (pose, vertices) -> layout.lines().forEach(line -> line.submit(pose, vertices)));
    }

    /** Submits a top-surface arrow in canonical south-facing space. */
    static void submitFrontArrow(PoseStack poseStack, SubmitNodeCollector collector) {
        // The arrow submits exactly three vertices. lightning uses a QUADS
        // vertex mode in 26.1.2, so an incomplete batch is silently dropped;
        // debugTriangleFan is the matching translucent triangle pipeline.
        collector.submitCustomGeometry(poseStack, RenderTypes.debugTriangleFan(), FRONT_ARROW::submit);
    }

    static FrontArrow frontArrow() {
        return FRONT_ARROW;
    }

    /**
     * Maps a physical hit in the presented block state back to the Extension's
     * unchanged upper-layer protocol coordinates.
     */
    static @Nullable Cell targetCell(double localX, double localY, double localZ) {
        return targetCell(localX, localY, localZ, Direction.NORTH);
    }

    static @Nullable Cell targetCell(double localX, double localY, double localZ, Direction facing) {
        if (localY < 1.0D / 16.0D || localY > 2.0D / 16.0D) {
            return null;
        }
        int x = (int) Math.floor(localX * 16.0D);
        int z = (int) Math.floor(localZ * 16.0D);
        if (!MoldPattern.isInnerCoordinate(x, z)) {
            return null;
        }
        return switch (facing) {
            case SOUTH -> new Cell(x, z);
            case WEST -> new Cell(z, 15 - x);
            case NORTH -> new Cell(15 - x, 15 - z);
            case EAST -> new Cell(15 - z, x);
            case UP, DOWN -> throw new IllegalArgumentException("Mold facing must be horizontal: " + facing);
        };
    }

    static int hoverPulseAlpha(float gameTicks) {
        float wave = ((float) Math.sin(gameTicks * 0.25F) + 1.0F) * 0.5F;
        return (int) (92.0F + 132.0F * wave);
    }

    /**
     * Builds the exact cells that the server-side eight-neighbour rule accepts
     * for the current pattern. A displayed cell is therefore always a legal
     * next carve while the target remains on a lodestone.
     */
    static Layout layout(MoldPattern pattern) {
        List<Cell> cells = new ArrayList<>();
        List<Cell> carvedCells = new ArrayList<>();
        Set<Line> lines = new LinkedHashSet<>();
        for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
            for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                Cell cell = new Cell(x, z);
                if (pattern.isCarved(x, z)) {
                    carvedCells.add(cell);
                }
                if (!pattern.canCarveAt(x, z)) {
                    continue;
                }
                cells.add(cell);
                lines.addAll(cell.outline());
            }
        }
        return new Layout(List.copyOf(cells), List.copyOf(lines), List.copyOf(carvedCells));
    }

    record Layout(List<Cell> cells, List<Line> lines, List<Cell> carvedCells,
                  List<HighlightCube> carvedFills) {
        static final Layout EMPTY = new Layout(List.of(), List.of(), List.of(), List.of());

        Layout(List<Cell> cells, List<Line> lines, List<Cell> carvedCells) {
            this(cells, lines, carvedCells, carvedFills(carvedCells));
        }

        private void submitCarvedFills(PoseStack.Pose pose, VertexConsumer vertices) {
            carvedFills.forEach(cube -> cube.submit(pose, vertices));
        }

        private static List<HighlightCube> carvedFills(List<Cell> carvedCells) {
            Set<Cell> carvedCellSet = Set.copyOf(carvedCells);
            return carvedCells.stream().map(cell -> cell.carvedHighlightCube(carvedCellSet)).toList();
        }
    }

    record Cell(int x, int z) {
        private List<Line> outline() {
            float minX = x * CELL;
            float maxX = (x + 1) * CELL;
            float minZ = z * CELL;
            float maxZ = (z + 1) * CELL;
            return List.of(
                    new Line(minX, SURFACE_Y, minZ, maxX, SURFACE_Y, minZ),
                    new Line(minX, SURFACE_Y, maxZ, maxX, SURFACE_Y, maxZ),
                    new Line(minX, SURFACE_Y, minZ, minX, SURFACE_Y, maxZ),
                    new Line(maxX, SURFACE_Y, minZ, maxX, SURFACE_Y, maxZ)
            );
        }

        HighlightCube carvedHighlightCube(Set<Cell> carvedCells) {
            boolean hasWestNeighbour = carvedCells.contains(new Cell(x - 1, z));
            boolean hasEastNeighbour = carvedCells.contains(new Cell(x + 1, z));
            boolean hasNorthNeighbour = carvedCells.contains(new Cell(x, z - 1));
            boolean hasSouthNeighbour = carvedCells.contains(new Cell(x, z + 1));
            float minX = x * CELL + (hasWestNeighbour ? 0.0F : CARVED_CUBE_INSET);
            float maxX = (x + 1) * CELL - (hasEastNeighbour ? 0.0F : CARVED_CUBE_INSET);
            float minZ = z * CELL + (hasNorthNeighbour ? 0.0F : CARVED_CUBE_INSET);
            float maxZ = (z + 1) * CELL - (hasSouthNeighbour ? 0.0F : CARVED_CUBE_INSET);
            return new HighlightCube(minX, CARVED_CUBE_BASE_Y, minZ,
                    maxX, CARVED_CUBE_BASE_Y + CELL, maxZ,
                    !hasWestNeighbour, !hasEastNeighbour, !hasNorthNeighbour, !hasSouthNeighbour);
        }

        private void submitHoverFill(PoseStack.Pose pose, VertexConsumer vertices, int alpha) {
            float minX = x * CELL + HOVER_FILL_INSET;
            float maxX = (x + 1) * CELL - HOVER_FILL_INSET;
            float minZ = z * CELL + HOVER_FILL_INSET;
            float maxZ = (z + 1) * CELL - HOVER_FILL_INSET;
            hoverVertex(pose, vertices, minX, HOVER_FILL_Y, minZ, alpha);
            hoverVertex(pose, vertices, minX, HOVER_FILL_Y, maxZ, alpha);
            hoverVertex(pose, vertices, maxX, HOVER_FILL_Y, maxZ, alpha);
            hoverVertex(pose, vertices, maxX, HOVER_FILL_Y, minZ, alpha);
        }

        private static void hoverVertex(PoseStack.Pose pose, VertexConsumer vertices,
                                        float x, float y, float z, int alpha) {
            vertices.addVertex(pose, x, y, z).setColor(255, 196, 64, alpha);
        }
    }

    /** A translucent voxel that fills the visual volume of one carved interior cell. */
    record HighlightCube(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                         boolean westFace, boolean eastFace, boolean northFace, boolean southFace) {
        private void submit(PoseStack.Pose pose, VertexConsumer vertices) {
            face(pose, vertices,
                    minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                    0.0F, 1.0F, 0.0F);
            face(pose, vertices,
                    minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                    0.0F, -1.0F, 0.0F);
            if (westFace) {
                face(pose, vertices,
                        minX, maxY, minZ, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ,
                        -1.0F, 0.0F, 0.0F);
            }
            if (eastFace) {
                face(pose, vertices,
                        maxX, maxY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ,
                        1.0F, 0.0F, 0.0F);
            }
            if (northFace) {
                face(pose, vertices,
                        maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ,
                        0.0F, 0.0F, -1.0F);
            }
            if (southFace) {
                face(pose, vertices,
                        minX, maxY, maxZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ,
                        0.0F, 0.0F, 1.0F);
            }
        }

        private static void face(PoseStack.Pose pose, VertexConsumer vertices,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 float x2, float y2, float z2, float x3, float y3, float z3,
                                 float normalX, float normalY, float normalZ) {
            vertex(pose, vertices, x0, y0, z0, normalX, normalY, normalZ);
            vertex(pose, vertices, x1, y1, z1, normalX, normalY, normalZ);
            vertex(pose, vertices, x2, y2, z2, normalX, normalY, normalZ);
            vertex(pose, vertices, x3, y3, z3, normalX, normalY, normalZ);
        }

        private static void vertex(PoseStack.Pose pose, VertexConsumer vertices,
                                   float x, float y, float z, float normalX, float normalY, float normalZ) {
            vertices.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, 96)
                    .setNormal(pose, normalX, normalY, normalZ);
        }
    }

    record Line(float startX, float startY, float startZ, float endX, float endY, float endZ,
                LineDirection direction) {
        Line(float startX, float startY, float startZ, float endX, float endY, float endZ) {
            this(startX, startY, startZ, endX, endY, endZ,
                    directionFor(startX, startY, startZ, endX, endY, endZ));
        }

        private void submit(PoseStack.Pose pose, VertexConsumer vertices) {
            vertex(pose, vertices, startX, startY, startZ, direction);
            vertex(pose, vertices, endX, endY, endZ, direction);
        }

        private static LineDirection directionFor(float startX, float startY, float startZ,
                                                  float endX, float endY, float endZ) {
            float x = endX - startX;
            float y = endY - startY;
            float z = endZ - startZ;
            float lengthSquared = x * x + y * y + z * z;
            if (lengthSquared == 0.0F) {
                throw new IllegalStateException("A carving-guide line must have non-zero length");
            }
            float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
            return new LineDirection(x * inverseLength, y * inverseLength, z * inverseLength);
        }

        private static void vertex(PoseStack.Pose pose, VertexConsumer vertices,
                                   float x, float y, float z, LineDirection direction) {
            vertices.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setNormal(pose, direction.x, direction.y, direction.z)
                    .setLineWidth(LINE_WIDTH);
        }
    }

    /** Direction consumed by the vanilla line shader to expand a line in screen space. */
    record LineDirection(float x, float y, float z) {
    }

    /** A short, flat translucent triangle outside the canonical front (+Z). */
    record FrontArrow(float baseLeftX, float baseLeftY, float baseLeftZ,
                      float baseRightX, float baseRightY, float baseRightZ,
                      float tipX, float tipY, float tipZ) {
        private void submit(PoseStack.Pose pose, VertexConsumer vertices) {
            vertex(pose, vertices, baseLeftX, baseLeftY, baseLeftZ);
            vertex(pose, vertices, tipX, tipY, tipZ);
            vertex(pose, vertices, baseRightX, baseRightY, baseRightZ);
        }

        private static void vertex(PoseStack.Pose pose, VertexConsumer vertices,
                                   float x, float y, float z) {
            vertices.addVertex(pose, x, y, z)
                    .setColor(64, 255, 96, FRONT_ARROW_ALPHA)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }
}
