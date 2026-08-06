package moe.liar.trms.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import moe.liar.trms.common.MoldWeaponAssembly;

/**
 * Texture- and Minecraft-independent topology for a mold mesh.
 *
 * <p>The returned quads are immutable and use block-local integer coordinates.
 * A quad's UVs are normalized to the complete sprite (0 or 1); the renderer
 * maps those values to the selected atlas sprite.</p>
 */
public final class MoldMeshTopology {
    private static final float MODEL_WIDTH = 16.0F;
    /** The visual fill sits one pixel above the ceramic base and visibly below the two-pixel rim. */
    static final float FILL_BASE_Y = 1.0F;
    /** A quarter model pixel of headroom makes a filled mold look intentionally not quite full. */
    static final float FILL_SURFACE_Y = 1.75F;
    /** Top height shared by the fixed ceramic rim and the solid interior cells. */
    static final float RIM_SURFACE_Y = 2.0F;
    /**
     * Sits just above the baked base floor so a carved empty cell can receive
     * the same client-side lighting treatment as its dynamic neighbours.
     */
    static final float CAVITY_FLOOR_Y = 1.001F;
    /** Keeps translucent fill walls off the solid ceramic cavity walls. */
    static final float FILL_SIDE_INSET = 0.001F;
    /** A solid weapon part is one model pixel thick and has closed geometry on every display side. */
    static final float WEAPON_PART_BASE_Y = 0.0F;
    static final float WEAPON_PART_SURFACE_Y = 1.0F;

    private MoldMeshTopology() {}

    public static List<Quad> build(MoldPattern pattern, boolean completeShell) {
        List<Quad> quads = new ArrayList<>();
        if (completeShell) {
            addBottomAndRim(quads, pattern);
        }
        addInterior(quads, pattern, false);
        return List.copyOf(quads);
    }

    /**
     * Builds world-only dynamic interior geometry, including a depth-biased
     * floor for every carved cell.
     *
     * <p>The block model owns the physical ceramic base at {@code y=1}. Its
     * static face is evaluated inside this block's two-pixel collision shape,
     * which makes the vanilla AO pipeline over-darken a visible carved floor.
     * The dynamic floor is one thousandth of a model pixel above that face, so
     * it replaces only its visible colour without coplanar depth fighting.</p>
     */
    static List<Quad> buildWorld(MoldPattern pattern) {
        List<Quad> quads = new ArrayList<>();
        addInterior(quads, pattern, true);
        return List.copyOf(quads);
    }

    /**
     * Builds the visual-only material occupying every carved cell.
     *
     * <p>Adjacent filled cells share a boundary, so only the outside side of
     * each connected component is emitted. The lower face is intentionally
     * omitted because the fixed ceramic base is directly below it.</p>
     */
    public static List<Quad> buildFill(MoldPattern pattern) {
        List<Quad> quads = new ArrayList<>();
        for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
            for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                if (!pattern.isCarved(x, z)) {
                    continue;
                }
                top(quads, x, z, x + 1, z + 1, FILL_SURFACE_Y);
                if (!pattern.isCarved(x - 1, z)) {
                    west(quads, x + FILL_SIDE_INSET, z + FILL_SIDE_INSET,
                            1.0F - 2.0F * FILL_SIDE_INSET, FILL_BASE_Y, FILL_SURFACE_Y);
                }
                if (!pattern.isCarved(x + 1, z)) {
                    east(quads, x + 1 - FILL_SIDE_INSET, z + FILL_SIDE_INSET,
                            1.0F - 2.0F * FILL_SIDE_INSET, FILL_BASE_Y, FILL_SURFACE_Y);
                }
                if (!pattern.isCarved(x, z - 1)) {
                    north(quads, x + FILL_SIDE_INSET, z + FILL_SIDE_INSET,
                            1.0F - 2.0F * FILL_SIDE_INSET, FILL_BASE_Y, FILL_SURFACE_Y);
                }
                if (!pattern.isCarved(x, z + 1)) {
                    south(quads, x + FILL_SIDE_INSET, z + 1 - FILL_SIDE_INSET,
                            1.0F - 2.0F * FILL_SIDE_INSET, FILL_BASE_Y, FILL_SURFACE_Y);
                }
            }
        }
        return List.copyOf(quads);
    }

    /**
     * Builds the closed one-pixel-thick silhouette used by a completed weapon-part item.
     *
     * <p>Unlike molten fill, a held or dropped part can be viewed from below,
     * so it includes its lower faces. Adjacent cells still share no internal
     * faces, preserving the continuous player-authored outline.</p>
     */
    public static List<Quad> buildWeaponPart(MoldPattern pattern) {
        List<Quad> quads = new ArrayList<>();
        for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
            for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                if (!pattern.isCarved(x, z)) {
                    continue;
                }
                top(quads, x, z, x + 1, z + 1, WEAPON_PART_SURFACE_Y);
                bottom(quads, x, z, x + 1, z + 1, WEAPON_PART_BASE_Y);
                if (!pattern.isCarved(x - 1, z)) {
                    west(quads, x, z, 1.0F, WEAPON_PART_BASE_Y, WEAPON_PART_SURFACE_Y);
                }
                if (!pattern.isCarved(x + 1, z)) {
                    east(quads, x + 1, z, 1.0F, WEAPON_PART_BASE_Y, WEAPON_PART_SURFACE_Y);
                }
                if (!pattern.isCarved(x, z - 1)) {
                    north(quads, x, z, 1.0F, WEAPON_PART_BASE_Y, WEAPON_PART_SURFACE_Y);
                }
                if (!pattern.isCarved(x, z + 1)) {
                    south(quads, x, z + 1, 1.0F, WEAPON_PART_BASE_Y, WEAPON_PART_SURFACE_Y);
                }
            }
        }
        return List.copyOf(quads);
    }

    /** Builds the one-pixel-wide, one-pixel-thick, ten-pixel-long handle used by assembled weapons. */
    public static List<Quad> buildHandle(int connectionX, int connectionZ) {
        float x0 = connectionX;
        float x1 = connectionX + MoldWeaponAssembly.HANDLE_WIDTH;
        float z0 = connectionZ;
        float z1 = connectionZ + MoldWeaponAssembly.HANDLE_LENGTH;
        List<Quad> quads = new ArrayList<>(6);
        top(quads, x0, z0, x1, z1, MoldWeaponAssembly.HANDLE_THICKNESS);
        bottom(quads, x0, z0, x1, z1, WEAPON_PART_BASE_Y);
        west(quads, x0, z0, z1 - z0, WEAPON_PART_BASE_Y, MoldWeaponAssembly.HANDLE_THICKNESS);
        east(quads, x1, z0, z1 - z0, WEAPON_PART_BASE_Y, MoldWeaponAssembly.HANDLE_THICKNESS);
        north(quads, x0, z0, x1 - x0, WEAPON_PART_BASE_Y, MoldWeaponAssembly.HANDLE_THICKNESS);
        south(quads, x0, z1, x1 - x0, WEAPON_PART_BASE_Y, MoldWeaponAssembly.HANDLE_THICKNESS);
        return List.copyOf(quads);
    }

    /**
     * Returns the quad after the mold's presentation turn around its vertical
     * center line. The stored pattern deliberately remains in its
     * protocol-stable coordinates; only the client presentation is rotated.
     *
     * <p>This is used for world-light sampling. The actual submitted world
     * mesh is rotated by its pose, so sampling the same oriented geometry keeps
     * ambient occlusion and block light on their physical, displayed sides.</p>
     */
    static Quad rotateForPresentation(Quad quad) {
        return rotateForPresentation(quad, Direction.NORTH);
    }

    /**
     * Matches the baked blockstate JSON {@code y} rotation and the pose used
     * by {@link MoldMeshBuilder#rotateWorldPresentation}. JSON's rotation
     * handedness is opposite to PoseStack's positive Y axis.
     */
    static Quad rotateForPresentation(Quad quad, Direction facing) {
        return switch (facing) {
            case SOUTH -> quad;
            case WEST -> new Quad(
                    MODEL_WIDTH - quad.z0(), quad.y0(), quad.x0(),
                    MODEL_WIDTH - quad.z1(), quad.y1(), quad.x1(),
                    MODEL_WIDTH - quad.z2(), quad.y2(), quad.x2(),
                    MODEL_WIDTH - quad.z3(), quad.y3(), quad.x3(),
                    -quad.nz(), quad.ny(), quad.nx()
            );
            case NORTH -> new Quad(
                    MODEL_WIDTH - quad.x0(), quad.y0(), MODEL_WIDTH - quad.z0(),
                    MODEL_WIDTH - quad.x1(), quad.y1(), MODEL_WIDTH - quad.z1(),
                    MODEL_WIDTH - quad.x2(), quad.y2(), MODEL_WIDTH - quad.z2(),
                    MODEL_WIDTH - quad.x3(), quad.y3(), MODEL_WIDTH - quad.z3(),
                    -quad.nx(), quad.ny(), -quad.nz()
            );
            case EAST -> new Quad(
                    quad.z0(), quad.y0(), MODEL_WIDTH - quad.x0(),
                    quad.z1(), quad.y1(), MODEL_WIDTH - quad.x1(),
                    quad.z2(), quad.y2(), MODEL_WIDTH - quad.x2(),
                    quad.z3(), quad.y3(), MODEL_WIDTH - quad.x3(),
                    quad.nz(), quad.ny(), -quad.nx()
            );
            case UP, DOWN -> throw new IllegalArgumentException("Mold facing must be horizontal: " + facing);
        };
    }

    private static void addBottomAndRim(List<Quad> quads, MoldPattern pattern) {
        top(quads, 0, 0, 16, 16, 1);
        bottom(quads, 0, 0, 16, 16, 0);
        west(quads, 0, 0, 16, 0, 1);
        east(quads, 16, 0, 16, 0, 1);
        north(quads, 0, 0, 16, 0, 1);
        south(quads, 0, 16, 16, 0, 1);

        for (int z = 0; z < 16; z++) {
            rimCell(quads, 0, z, pattern);
            rimCell(quads, 15, z, pattern);
        }
        for (int x = 1; x < 15; x++) {
            rimCell(quads, x, 0, pattern);
            rimCell(quads, x, 15, pattern);
        }
    }

    private static void addInterior(List<Quad> quads, MoldPattern pattern, boolean includeCarvedFloors) {
        for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
            for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                if (pattern.isCarved(x, z)) {
                    if (includeCarvedFloors) {
                        top(quads, x, z, x + 1, z + 1, CAVITY_FLOOR_Y);
                    }
                    continue;
                }
                top(quads, x, z, x + 1, z + 1, RIM_SURFACE_Y);
                if (pattern.isCarved(x - 1, z)) west(quads, x, z, 1, FILL_BASE_Y, RIM_SURFACE_Y);
                if (pattern.isCarved(x + 1, z)) east(quads, x + 1, z, 1, FILL_BASE_Y, RIM_SURFACE_Y);
                if (pattern.isCarved(x, z - 1)) north(quads, x, z, 1, FILL_BASE_Y, RIM_SURFACE_Y);
                if (pattern.isCarved(x, z + 1)) south(quads, x, z + 1, 1, FILL_BASE_Y, RIM_SURFACE_Y);
            }
        }
    }

    private static void rimCell(List<Quad> quads, int x, int z, MoldPattern pattern) {
        top(quads, x, z, x + 1, z + 1, 2);
        if (x == 0) {
            west(quads, x, z, 1, 1, 2);
            if (z > 0 && z < 15 && pattern.isCarved(1, z)) east(quads, 1, z, 1, 1, 2);
        }
        if (x == 15) {
            east(quads, 16, z, 1, 1, 2);
            if (z > 0 && z < 15 && pattern.isCarved(14, z)) west(quads, 15, z, 1, 1, 2);
        }
        if (z == 0) {
            north(quads, x, z, 1, 1, 2);
            if (x > 0 && x < 15 && pattern.isCarved(x, 1)) south(quads, x, 1, 1, 1, 2);
        }
        if (z == 15) {
            south(quads, x, 16, 1, 1, 2);
            if (x > 0 && x < 15 && pattern.isCarved(x, 14)) north(quads, x, 15, 1, 1, 2);
        }
    }

    private static void top(List<Quad> q, float x0, float z0, float x1, float z1, float y) {
        // Keep the vertex order identical to FaceInfo.UP.  Custom geometry
        // receives four unindexed vertices, so the canonical order is needed
        // for the renderer to form both triangles of a quad consistently.
        q.add(new Quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, 0, 1, 0));
    }

    private static void bottom(List<Quad> q, float x0, float z0, float x1, float z1, float y) {
        q.add(new Quad(x0, y, z1, x0, y, z0, x1, y, z0, x1, y, z1, 0, -1, 0));
    }

    private static void west(List<Quad> q, float x, float z, float width, float y0, float y1) {
        q.add(new Quad(x, y1, z, x, y0, z, x, y0, z + width, x, y1, z + width, -1, 0, 0));
    }

    private static void east(List<Quad> q, float x, float z, float width, float y0, float y1) {
        q.add(new Quad(x, y1, z + width, x, y0, z + width, x, y0, z, x, y1, z, 1, 0, 0));
    }

    private static void north(List<Quad> q, float x, float z, float width, float y0, float y1) {
        q.add(new Quad(x + width, y1, z, x + width, y0, z, x, y0, z, x, y1, z, 0, 0, -1));
    }

    private static void south(List<Quad> q, float x, float z, float width, float y0, float y1) {
        q.add(new Quad(x, y1, z, x, y0, z, x + width, y0, z, x + width, y1, z, 0, 0, 1));
    }

    public record Quad(float x0, float y0, float z0, float x1, float y1, float z1,
                       float x2, float y2, float z2, float x3, float y3, float z3,
                       float nx, float ny, float nz) {
        public Vertex vertex(int index, int overlay, int light) {
            if (index < 0 || index > 3) {
                throw new IllegalArgumentException("Quad vertex index must be 0..3: " + index);
            }
            float x = switch (index) { case 0 -> x0; case 1 -> x1; case 2 -> x2; default -> x3; };
            float y = switch (index) { case 0 -> y0; case 1 -> y1; case 2 -> y2; default -> y3; };
            float z = switch (index) { case 0 -> z0; case 1 -> z1; case 2 -> z2; default -> z3; };
            TextureCoordinates texture = textureCoordinates(x, y, z, nx, ny, nz);
            return new Vertex(x, y, z, texture.u(), texture.v(), overlay, light, nx, ny, nz);
        }
    }

    /**
     * Maps every dynamic quad into one continuous 16×16 mold texture space.
     *
     * <p>Mapping a complete sprite to every one-pixel cell repeats the sprite
     * 196 times across a mold. At normal viewing distances that aliases into a
     * striped moire pattern and makes the item model appear to disappear. The
     * static rim is one continuous model face, so the dynamic mesh must use the
     * same whole-mold scale.</p>
     */
    static TextureCoordinates textureCoordinates(float x, float y, float z,
                                                 float nx, float ny, float nz) {
        if (ny != 0.0f) {
            return new TextureCoordinates(x / 16.0f, z / 16.0f);
        }
        if (nx != 0.0f) {
            return new TextureCoordinates(z / 16.0f, 1.0f - y / 2.0f);
        }
        if (nz != 0.0f) {
            return new TextureCoordinates(x / 16.0f, 1.0f - y / 2.0f);
        }
        throw new IllegalArgumentException("A mold quad must have a non-zero normal");
    }

    record TextureCoordinates(float u, float v) {}

    /** Complete attributes required by the entitySolid vertex format. */
    public record Vertex(float x, float y, float z, float u, float v,
                         int overlay, int light, float nx, float ny, float nz) {}
}
