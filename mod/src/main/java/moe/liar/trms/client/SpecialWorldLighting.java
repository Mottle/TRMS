package moe.liar.trms.client;

import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ao.EnhancedBlockModelLighter;
import org.joml.Vector3f;

/**
 * Vanilla-compatible lighting sampler for block-like geometry submitted by a
 * block-entity renderer.
 *
 * <p>Implementors only need to expose block-local quads. The sampler mirrors
 * {@code ModelBlockRenderer}: emitting states use flat face lighting, while
 * non-emitting states use the vanilla AO interpolation. The resulting vertex
 * colour and packed light arrays can then be consumed by a moving-block
 * RenderType without invoking entity normal lighting.</p>
 */
final class SpecialWorldLighting {
    private SpecialWorldLighting() {
    }

    static Result capture(
            BlockAndTintGetter level,
            BlockPos position,
            BlockState blockState,
            List<? extends SpecialWorldQuad> sourceQuads,
            TextureAtlasSprite sprite
    ) {
        return capture(level, position, blockState, sourceQuads, sprite, SpecialWorldRenderTypes.solid());
    }

    static Result capture(
            BlockAndTintGetter level,
            BlockPos position,
            BlockState blockState,
            List<? extends SpecialWorldQuad> sourceQuads,
            TextureAtlasSprite sprite,
            SpecialWorldRenderTypes.Layer layer
    ) {
        return capture(level, position, blockState, sourceQuads, sprite, layer, true);
    }

    static Result capture(
            BlockAndTintGetter level,
            BlockPos position,
            BlockState blockState,
            List<? extends SpecialWorldQuad> sourceQuads,
            TextureAtlasSprite sprite,
            SpecialWorldRenderTypes.Layer layer,
            boolean ambientOcclusion
    ) {
        int[] colors = new int[sourceQuads.size() * 4];
        int[] lights = new int[sourceQuads.size() * 4];
        BlockModelLighter lighter = EnhancedBlockModelLighter.newInstance();
        lighter.reset();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
                sprite,
                layer.chunkLayer(),
                layer.renderType(),
                -1,
                true,
                0,
                ambientOcclusion
        );
        boolean useAmbientOcclusion = shouldUseAmbientOcclusion(
                blockState.getLightEmission(level, position), material.ambientOcclusion());

        for (int faceIndex = 0; faceIndex < sourceQuads.size(); faceIndex++) {
            SpecialWorldQuad quadData = sourceQuads.get(faceIndex);
            BakedQuad quad = bakedQuad(quadData, material);
            QuadInstance sampled = new QuadInstance();
            if (useAmbientOcclusion && material.ambientOcclusion()) {
                lighter.prepareQuadAmbientOcclusion(level, blockState, position, quad, sampled);
            } else {
                int lightCoords = lighter.getLightCoords(
                        blockState, level, position.relative(quad.direction()));
                lighter.prepareQuadFlat(level, blockState, position, lightCoords, quad, sampled);
            }
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                int offset = faceIndex * 4 + vertexIndex;
                colors[offset] = sampled.getColor(vertexIndex);
                lights[offset] = sampled.getLightCoords(vertexIndex);
            }
        }
        return new Result(colors, lights);
    }

    static boolean shouldUseAmbientOcclusion(int lightEmission, boolean materialAmbientOcclusion) {
        return lightEmission == 0 && materialAmbientOcclusion;
    }

    /** Immutable per-vertex attributes produced by the vanilla light sampler. */
    static final class Result {
        private final int[] colors;
        private final int[] lights;

        private Result(int[] colors, int[] lights) {
            this.colors = colors;
            this.lights = lights;
        }

        int color(int faceIndex, int vertexIndex) {
            return colors[faceIndex * 4 + vertexIndex];
        }

        int light(int faceIndex, int vertexIndex) {
            return lights[faceIndex * 4 + vertexIndex];
        }
    }

    private static BakedQuad bakedQuad(SpecialWorldQuad quad, BakedQuad.MaterialInfo material) {
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

    private static Direction direction(SpecialWorldQuad quad) {
        if (quad.nx() > 0.0f) return Direction.EAST;
        if (quad.nx() < 0.0f) return Direction.WEST;
        if (quad.ny() > 0.0f) return Direction.UP;
        if (quad.ny() < 0.0f) return Direction.DOWN;
        if (quad.nz() > 0.0f) return Direction.SOUTH;
        if (quad.nz() < 0.0f) return Direction.NORTH;
        throw new IllegalArgumentException("A special-world quad must have a non-zero normal");
    }
}
