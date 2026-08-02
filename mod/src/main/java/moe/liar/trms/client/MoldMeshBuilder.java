package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ao.EnhancedBlockModelLighter;
import org.joml.Vector3f;

/** Pure client quad builder shared by world and item rendering. */
public final class MoldMeshBuilder {
    /*
     * TextureAtlas.LOCATION_BLOCKS is deprecated in 26.1.2 even though the
     * atlas manager still identifies this atlas by its texture location.
     * Keep the stable vanilla identifier locally instead of binding runtime
     * rendering to that deprecated compatibility constant.
     */
    private static final Identifier BLOCK_ATLAS_TEXTURE =
            Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    public static final SpriteId TERRACOTTA_SPRITE = new SpriteId(
            BLOCK_ATLAS_TEXTURE,
            Identifier.fromNamespaceAndPath("minecraft", "block/terracotta")
    );
    /**
     * Project-owned greyscale copies of the vanilla animated lava frames.
     *
     * <p>Vertex tinting is multiplicative. Applying a silver tint to the
     * orange vanilla source therefore still produces orange. These neutral
     * source frames let every fill material supply its actual hue while
     * retaining Minecraft's lava animation cadence.</p>
     */
    public static final SpriteId MOLTEN_STILL_SPRITE = new SpriteId(
            BLOCK_ATLAS_TEXTURE,
            Identifier.fromNamespaceAndPath("trms", "block/molten_still")
    );
    public static final SpriteId MOLTEN_FLOW_SPRITE = new SpriteId(
            BLOCK_ATLAS_TEXTURE,
            Identifier.fromNamespaceAndPath("trms", "block/molten_flow")
    );
    static final int FULL_BRIGHT_LIGHT = 0x00F000F0;
    public static final org.joml.Vector3fc[] ITEM_EXTENTS = {
            new org.joml.Vector3f(-0.5f, -0.0625f, -0.5f),
            new org.joml.Vector3f(0.5f, 0.0625f, 0.5f)
    };
    /**
     * Raw block-model-space bounds for the dropped-item renderer.
     *
     * <p>{@link net.minecraft.client.resources.model.cuboid.ItemTransform}
     * already centers explicit display transforms around {@code 0.5}.  Ground
     * rendering therefore must submit both these raw extents and raw geometry,
     * otherwise the item entity spins around an offset point.</p>
     */
    public static final org.joml.Vector3fc[] GROUND_ITEM_EXTENTS = {
            new org.joml.Vector3f(0.0f, 0.0f, 0.0f),
            new org.joml.Vector3f(1.0f, 0.125f, 1.0f)
    };

    private MoldMeshBuilder() {}

    public static TextureAtlasSprite currentTerracottaSprite() {
        return Minecraft.getInstance().getAtlasManager().get(TERRACOTTA_SPRITE);
    }

    /** Applies the directional presentation turn to block-local world geometry. */
    static void rotateWorldPresentation(PoseStack poseStack) {
        rotateWorldPresentation(poseStack, Direction.NORTH);
    }

    /** Rotates the canonical south-facing mold geometry to its horizontal block state. */
    static void rotateWorldPresentation(PoseStack poseStack, Direction facing) {
        // Blockstate JSON y rotations use the opposite handedness from
        // PoseStack's positive Y axis. Negate to keep dynamic geometry aligned
        // with the baked static shell for every horizontal state.
        rotateYAround(poseStack, -facing.toYRot(), 0.5F, 0.5F);
    }

    public static Mesh buildWorld(MoldPattern pattern, TextureAtlasSprite sprite) {
        return build(pattern, sprite, false, RenderTypes.entitySolid(sprite.atlasLocation()));
    }

    public static Mesh buildItem(MoldPattern pattern, TextureAtlasSprite sprite) {
        return build(pattern, sprite, true, RenderTypes.entitySolid(sprite.atlasLocation()));
    }

    /**
     * Small render-thread LRU cache shared by one renderer instance.
     *
     * <p>Several visible molds normally have different immutable patterns. A
     * one-entry cache consequently rebuilt A, then B, on every frame. This
     * cache remains deliberately bounded, and a changed atlas-sprite identity
     * clears all entries so a resource reload cannot retain old sprites.</p>
     */
    public static final class Cache {
        private static final int MAXIMUM_MESHES = 64;

        private final boolean completeShell;
        private TextureAtlasSprite sprite;
        private final Map<MoldPattern, Mesh> meshes = boundedMeshMap();
        private TextureAtlasSprite fillTopSprite;
        private TextureAtlasSprite fillSideSprite;
        private final Map<MoldPattern, Mesh> fillMeshes = boundedMeshMap();

        public Cache(boolean completeShell) {
            this.completeShell = completeShell;
        }

        /**
         * Returns geometry for an immutable pattern.
         *
         * <p>{@code revision} deliberately is not part of the key: it guards
         * network state freshness, whereas this mesh's shape is fully defined
         * by the pattern, selected sprite, and this cache's shell mode.</p>
         */
        public Mesh get(MoldPattern nextPattern, long nextRevision, TextureAtlasSprite nextSprite) {
            Objects.requireNonNull(nextPattern, "nextPattern");
            Objects.requireNonNull(nextSprite, "nextSprite");
            if (nextSprite != sprite) {
                sprite = nextSprite;
                meshes.clear();
            }
            return meshes.computeIfAbsent(nextPattern,
                    pattern -> completeShell ? buildItem(pattern, sprite) : buildWorld(pattern, sprite));
        }

        /** Returns contiguous visual fill geometry, cached independently of the ceramic shell. */
        public Mesh getFill(MoldPattern pattern, TextureAtlasSprite topSprite, TextureAtlasSprite sideSprite) {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(topSprite, "topSprite");
            Objects.requireNonNull(sideSprite, "sideSprite");
            if (topSprite != fillTopSprite || sideSprite != fillSideSprite) {
                fillTopSprite = topSprite;
                fillSideSprite = sideSprite;
                fillMeshes.clear();
            }
            return fillMeshes.computeIfAbsent(pattern,
                    nextPattern -> buildFill(nextPattern, fillTopSprite, fillSideSprite));
        }

        private static Map<MoldPattern, Mesh> boundedMeshMap() {
            return new LinkedHashMap<>(MAXIMUM_MESHES, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<MoldPattern, Mesh> eldest) {
                    return size() > MAXIMUM_MESHES;
                }
            };
        }

    }

    /** Immutable render-thread data: no pattern enumeration or quad construction occurs in submit. */
    public static final class Mesh {
        private final List<Face> faces;
        private final RenderType renderType;

        private Mesh(List<Face> faces, RenderType renderType) {
            this.faces = List.copyOf(faces);
            this.renderType = renderType;
        }

        public void submitWorld(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                WorldLighting lighting) {
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, vertices) -> render(pose, vertices, light, OverlayTexture.NO_OVERLAY,
                            1.0f / 16.0f, lighting, 0xFFFFFFFF));
        }

        /** Submits a material fill with fixed level-15 block and sky light. */
        public void submitFullBrightWorld(PoseStack poseStack, SubmitNodeCollector collector, int color) {
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, vertices) -> render(pose, vertices, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY,
                            1.0f / 16.0f, null, color));
        }

        public void submitItem(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
            poseStack.pushPose();
            centerItemGeometry(poseStack);
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, vertices) -> render(pose, vertices, light, overlay, 1.0f / 16.0f, null, 0xFFFFFFFF));
            poseStack.popPose();
        }

        /**
         * Submits raw block-model-space geometry for the {@code ground}
         * display context.  The context's {@code ItemTransform} supplies the
         * one and only centering translation before an item entity rotates.
         */
        public void submitGroundItem(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
            poseStack.pushPose();
            // Ground special rendering deliberately uses raw 0..1 model space;
            // rotate around that raw model center so an ItemEntity retains its
            // already-correct spin center.
            rotateYAround(poseStack, 180.0F, 0.5F, 0.5F);
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, vertices) -> render(pose, vertices, light, overlay, 1.0f / 16.0f, null, 0xFFFFFFFF));
            poseStack.popPose();
        }

        private void render(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay, float scale,
                            WorldLighting lighting, int tint) {
            for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
                Face face = faces.get(faceIndex);
                for (int index = 0; index < 4; index++) {
                    int vertexLight = lighting == null ? light : lighting.light(faceIndex, index);
                    int vertexColor = lighting == null ? tint : lighting.color(faceIndex, index);
                    face.vertex(vertices, pose, index, overlay, vertexLight, vertexColor, scale);
                }
            }
        }
    }

    /**
     * Centers custom 0..1 block-space geometry in a hand, frame, or fallback
     * item transform without changing the placed mold's canonical x/z facing.
     */
    static void centerItemGeometry(PoseStack poseStack) {
        poseStack.translate(-0.5F, -0.0625F, -0.5F);
    }

    private record Face(float x0, float y0, float z0, float u0, float v0,
                        float x1, float y1, float z1, float u1, float v1,
                        float x2, float y2, float z2, float u2, float v2,
                        float x3, float y3, float z3, float u3, float v3,
                        float nx, float ny, float nz) {
        private void vertex(VertexConsumer vertices, PoseStack.Pose pose, int index, int overlay, int light,
                            int color, float scale) {
            if (index < 0 || index > 3) {
                throw new IllegalArgumentException("Face vertex index must be 0..3: " + index);
            }
            float x = switch (index) { case 0 -> x0; case 1 -> x1; case 2 -> x2; default -> x3; };
            float y = switch (index) { case 0 -> y0; case 1 -> y1; case 2 -> y2; default -> y3; };
            float z = switch (index) { case 0 -> z0; case 1 -> z1; case 2 -> z2; default -> z3; };
            float u = switch (index) { case 0 -> u0; case 1 -> u1; case 2 -> u2; default -> u3; };
            float v = switch (index) { case 0 -> v0; case 1 -> v1; case 2 -> v2; default -> v3; };
            vertices.addVertex(pose, x * scale, y * scale, z * scale)
                    // The four-channel overload makes the ARGB convention
                    // explicit at this renderer boundary. It also prevents a
                    // future packed-colour API change from silently dropping
                    // the material tint.
                    .setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color))
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setUv2(light & 0xFFFF, light >>> 16)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    /** Per-vertex world lighting sampled by the same lighter as static block models. */
    public static final class WorldLighting {
        private final int[] colors;
        private final int[] lights;

        private WorldLighting(int[] colors, int[] lights) {
            this.colors = colors;
            this.lights = lights;
        }

        private int color(int faceIndex, int vertexIndex) {
            return colors[faceIndex * 4 + vertexIndex];
        }

        private int light(int faceIndex, int vertexIndex) {
            return lights[faceIndex * 4 + vertexIndex];
        }
    }

    /**
     * Samples ambient occlusion and lightmap coordinates exactly as the static
     * mold block model does.  Custom block-entity geometry bypasses chunk model
     * baking, so it must request these attributes explicitly.
     */
    public static WorldLighting captureWorldLighting(BlockAndTintGetter level, BlockPos position,
                                                     BlockState blockState, MoldPattern pattern,
                                                     TextureAtlasSprite sprite) {
        return captureWorldLighting(level, position, blockState, pattern, sprite, Direction.NORTH);
    }

    /** Samples lighting after applying the same horizontal turn used for world submission. */
    public static WorldLighting captureWorldLighting(BlockAndTintGetter level, BlockPos position,
                                                     BlockState blockState, MoldPattern pattern,
                                                     TextureAtlasSprite sprite, Direction facing) {
        List<MoldMeshTopology.Quad> sourceQuads = MoldMeshTopology.build(pattern, false);
        List<MoldMeshTopology.Quad> quads = new ArrayList<>(sourceQuads.size());
        for (MoldMeshTopology.Quad quad : sourceQuads) {
            quads.add(MoldMeshTopology.rotateForPresentation(quad, facing));
        }
        int[] colors = new int[quads.size() * 4];
        int[] lights = new int[quads.size() * 4];
        BlockModelLighter lighter = EnhancedBlockModelLighter.newInstance();
        lighter.reset();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
                sprite,
                ChunkSectionLayer.SOLID,
                RenderTypes.entitySolid(sprite.atlasLocation()),
                -1,
                true,
                0,
                true
        );

        for (int faceIndex = 0; faceIndex < quads.size(); faceIndex++) {
            BakedQuad quad = bakedQuad(quads.get(faceIndex), material);
            QuadInstance sampled = new QuadInstance();
            lighter.prepareQuadAmbientOcclusion(level, blockState, position, quad, sampled);
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                int offset = faceIndex * 4 + vertexIndex;
                colors[offset] = sampled.getColor(vertexIndex);
                lights[offset] = sampled.getLightCoords(vertexIndex);
            }
        }
        return new WorldLighting(colors, lights);
    }

    private static BakedQuad bakedQuad(MoldMeshTopology.Quad quad, BakedQuad.MaterialInfo material) {
        return new BakedQuad(
                new Vector3f(quad.x0() / 16.0f, quad.y0() / 16.0f, quad.z0() / 16.0f),
                new Vector3f(quad.x1() / 16.0f, quad.y1() / 16.0f, quad.z1() / 16.0f),
                new Vector3f(quad.x2() / 16.0f, quad.y2() / 16.0f, quad.z2() / 16.0f),
                new Vector3f(quad.x3() / 16.0f, quad.y3() / 16.0f, quad.z3() / 16.0f),
                0L, 0L, 0L, 0L,
                direction(quad),
                material
        );
    }

    private static Direction direction(MoldMeshTopology.Quad quad) {
        if (quad.nx() > 0.0f) return Direction.EAST;
        if (quad.nx() < 0.0f) return Direction.WEST;
        if (quad.ny() > 0.0f) return Direction.UP;
        if (quad.ny() < 0.0f) return Direction.DOWN;
        if (quad.nz() > 0.0f) return Direction.SOUTH;
        if (quad.nz() < 0.0f) return Direction.NORTH;
        throw new IllegalArgumentException("A mold quad must have a non-zero normal");
    }

    private static void rotateYAround(PoseStack poseStack, float degrees, float centerX, float centerZ) {
        poseStack.translate(centerX, 0.0F, centerZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-centerX, 0.0F, -centerZ);
    }

    private static Mesh build(MoldPattern pattern, TextureAtlasSprite sprite, boolean completeShell,
                              RenderType renderType) {
        // In 26.1, getU/getV take a normalized 0..1 offset.  The explicit
        // endpoints avoid the old 0..16 convention accidentally sampling
        // sixteen neighbouring atlas sprites.
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        List<MoldMeshTopology.Quad> topology = MoldMeshTopology.build(pattern, completeShell);
        List<Face> faces = new ArrayList<>(topology.size());
        for (MoldMeshTopology.Quad quad : topology) {
            faces.add(face(quad, u0, u1, v0, v1));
        }
        return new Mesh(faces, renderType);
    }

    private static Mesh buildFill(MoldPattern pattern, TextureAtlasSprite topSprite,
                                  TextureAtlasSprite sideSprite) {
        List<MoldMeshTopology.Quad> topology = MoldMeshTopology.buildFill(pattern);
        List<Face> faces = new ArrayList<>(topology.size());
        for (MoldMeshTopology.Quad quad : topology) {
            TextureAtlasSprite sprite = quad.ny() > 0.0F ? topSprite : sideSprite;
            faces.add(face(quad, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1()));
        }
        return new Mesh(faces, RenderTypes.entityTranslucent(topSprite.atlasLocation()));
    }

    private static Face face(MoldMeshTopology.Quad quad, float spriteU0, float spriteU1,
                             float spriteV0, float spriteV1) {
        MoldMeshTopology.TextureCoordinates first = textureCoordinates(quad.x0(), quad.y0(), quad.z0(), quad);
        MoldMeshTopology.TextureCoordinates second = textureCoordinates(quad.x1(), quad.y1(), quad.z1(), quad);
        MoldMeshTopology.TextureCoordinates third = textureCoordinates(quad.x2(), quad.y2(), quad.z2(), quad);
        MoldMeshTopology.TextureCoordinates fourth = textureCoordinates(quad.x3(), quad.y3(), quad.z3(), quad);
        return new Face(
                quad.x0(), quad.y0(), quad.z0(), interpolate(spriteU0, spriteU1, first.u()), interpolate(spriteV0, spriteV1, first.v()),
                quad.x1(), quad.y1(), quad.z1(), interpolate(spriteU0, spriteU1, second.u()), interpolate(spriteV0, spriteV1, second.v()),
                quad.x2(), quad.y2(), quad.z2(), interpolate(spriteU0, spriteU1, third.u()), interpolate(spriteV0, spriteV1, third.v()),
                quad.x3(), quad.y3(), quad.z3(), interpolate(spriteU0, spriteU1, fourth.u()), interpolate(spriteV0, spriteV1, fourth.v()),
                quad.nx(), quad.ny(), quad.nz()
        );
    }

    private static MoldMeshTopology.TextureCoordinates textureCoordinates(float x, float y, float z,
                                                                            MoldMeshTopology.Quad quad) {
        return MoldMeshTopology.textureCoordinates(x, y, z, quad.nx(), quad.ny(), quad.nz());
    }

    private static float interpolate(float start, float end, float factor) {
        return start + (end - start) * factor;
    }
}
