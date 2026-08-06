package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

class SpecialWorldRenderTypesTest {
    @Test
    void pairsMovingBlockRenderTypesWithMatchingChunkLayers() {
        assertSame(SpecialWorldRenderTypes.Layer.SOLID,
                SpecialWorldRenderTypes.solid());
        assertSame(SpecialWorldRenderTypes.Layer.TRANSLUCENT,
                SpecialWorldRenderTypes.translucent());
        assertEquals(ChunkSectionLayer.SOLID,
                SpecialWorldRenderTypes.solid().chunkLayer());
        assertEquals(ChunkSectionLayer.TRANSLUCENT,
                SpecialWorldRenderTypes.translucent().chunkLayer());
    }
}
