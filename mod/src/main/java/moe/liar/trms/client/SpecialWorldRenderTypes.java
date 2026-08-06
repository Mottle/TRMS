package moe.liar.trms.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Render layers for block-like geometry submitted by a block-entity renderer.
 *
 * <p>World special geometry must use Minecraft's moving-block layers rather
 * than entity layers.  The moving-block layers use the block vertex format
 * and block shader, which consumes the vertex colour and lightmap exactly like
 * a baked chunk model.  Entity layers apply an additional normal-based light
 * calculation and therefore are appropriate for items/entities, but not for
 * geometry that was already lit with {@code BlockModelLighter}.</p>
 */
final class SpecialWorldRenderTypes {
    private SpecialWorldRenderTypes() {
    }

    static Layer solid() {
        return Layer.SOLID;
    }

    static Layer translucent() {
        return Layer.TRANSLUCENT;
    }

    enum Layer {
        SOLID(RenderTypes.solidMovingBlock(), ChunkSectionLayer.SOLID),
        TRANSLUCENT(RenderTypes.translucentMovingBlock(), ChunkSectionLayer.TRANSLUCENT);

        private final RenderType renderType;
        private final ChunkSectionLayer chunkLayer;

        Layer(RenderType renderType, ChunkSectionLayer chunkLayer) {
            this.renderType = renderType;
            this.chunkLayer = chunkLayer;
        }

        RenderType renderType() {
            return renderType;
        }

        ChunkSectionLayer chunkLayer() {
            return chunkLayer;
        }
    }
}
