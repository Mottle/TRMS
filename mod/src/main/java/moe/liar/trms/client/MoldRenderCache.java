package moe.liar.trms.client;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Per-block-entity render data that remains valid across the short-lived
 * {@link MoldBlockEntityRenderState} snapshots created by the 26.1 renderer.
 *
 * <p>This cache deliberately stores no {@code Level}, {@code BlockPos}, or
 * block-entity reference. It is owned by its client block entity, so chunk
 * unloading and changing worlds releases it naturally. The sprite key is
 * weak: a resource reload can therefore discard its old atlas without waiting
 * for every loaded mold to become visible again.</p>
 */
final class MoldRenderCache {
    private MoldPattern carvingGuidePattern = MoldPattern.EMPTY;
    private long carvingGuideRevision = Long.MIN_VALUE;
    private MoldCarvingGuide.Layout carvingGuide = MoldCarvingGuide.Layout.EMPTY;

    private MoldPattern lightingPattern = MoldPattern.EMPTY;
    private long lightingRevision = Long.MIN_VALUE;
    private int lightingCoords = Integer.MIN_VALUE;
    private WeakReference<TextureAtlasSprite> lightingSprite = new WeakReference<>(null);
    private Direction lightingFacing;
    private MoldMeshBuilder.WorldLighting worldLighting;
    private boolean hasWorldLighting;

    /**
     * Returns the immutable legal-carve layout for the current authoritative
     * mold state. Callers should invoke this only when the guide will be shown.
     */
    synchronized MoldCarvingGuide.Layout carvingGuide(MoldPattern pattern, long revision) {
        if (!pattern.equals(carvingGuidePattern) || revision != carvingGuideRevision) {
            carvingGuidePattern = pattern;
            carvingGuideRevision = revision;
            carvingGuide = MoldCarvingGuide.layout(pattern);
        }
        return carvingGuide;
    }

    /**
     * Returns immutable ambient-occlusion data, recapturing it only when a
     * geometry, lighting, facing, or atlas-sprite input changed.
     *
     * <p>The supplier is intentionally not retained; it may close over the
     * live client level for this one extraction only.</p>
     */
    synchronized MoldMeshBuilder.WorldLighting worldLighting(
            MoldPattern pattern,
            long revision,
            int lightCoords,
            TextureAtlasSprite sprite,
            Direction facing,
            Supplier<MoldMeshBuilder.WorldLighting> capture
    ) {
        if (!hasWorldLighting
                || !pattern.equals(lightingPattern)
                || revision != lightingRevision
                || lightCoords != lightingCoords
                || lightingSprite.get() != sprite
                || lightingFacing != facing) {
            lightingPattern = pattern;
            lightingRevision = revision;
            lightingCoords = lightCoords;
            lightingSprite = new WeakReference<>(sprite);
            lightingFacing = facing;
            worldLighting = capture.get();
            hasWorldLighting = true;
        }
        return worldLighting;
    }

    /** Drops cached arrays promptly when the client receives new mold state. */
    synchronized void invalidate() {
        carvingGuidePattern = MoldPattern.EMPTY;
        carvingGuideRevision = Long.MIN_VALUE;
        carvingGuide = MoldCarvingGuide.Layout.EMPTY;
        lightingPattern = MoldPattern.EMPTY;
        lightingRevision = Long.MIN_VALUE;
        lightingCoords = Integer.MIN_VALUE;
        lightingSprite = new WeakReference<>(null);
        lightingFacing = null;
        worldLighting = null;
        hasWorldLighting = false;
    }
}
