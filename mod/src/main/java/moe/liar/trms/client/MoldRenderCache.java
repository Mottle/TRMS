package moe.liar.trms.client;

import java.lang.ref.WeakReference;
import java.util.function.LongSupplier;
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
    private long lightingFingerprint = Long.MIN_VALUE;
    private WeakReference<TextureAtlasSprite> lightingSprite = new WeakReference<>(null);
    private Direction lightingFacing;
    private SpecialWorldLighting.Result worldLighting;
    private boolean hasWorldLighting;

    private MoldPattern fillLightingPattern = MoldPattern.EMPTY;
    private long fillLightingRevision = Long.MIN_VALUE;
    private long fillLightingFingerprint = Long.MIN_VALUE;
    private WeakReference<TextureAtlasSprite> fillLightingSprite = new WeakReference<>(null);
    private Direction fillLightingFacing;
    private SpecialWorldLighting.Result fillWorldLighting;
    private boolean hasFillWorldLighting;
    private long fingerprintGameTime = Long.MIN_VALUE;
    private long cachedFingerprint = Long.MIN_VALUE;

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

    /** Computes the neighbourhood light signature at most once per game tick. */
    synchronized long lightingFingerprint(long gameTime, LongSupplier capture) {
        if (gameTime != fingerprintGameTime) {
            fingerprintGameTime = gameTime;
            cachedFingerprint = capture.getAsLong();
        }
        return cachedFingerprint;
    }

    /**
     * Returns immutable ambient-occlusion data, recapturing it only when a
     * geometry, lighting, facing, or atlas-sprite input changed.
     *
     * <p>The supplier is intentionally not retained; it may close over the
     * live client level for this one extraction only.</p>
     */
    synchronized SpecialWorldLighting.Result worldLighting(
            MoldPattern pattern,
            long revision,
            long lightingFingerprint,
            TextureAtlasSprite sprite,
            Direction facing,
            Supplier<SpecialWorldLighting.Result> capture
    ) {
        if (!hasWorldLighting
                || !pattern.equals(lightingPattern)
                || revision != lightingRevision
                || lightingFingerprint != this.lightingFingerprint
                || lightingSprite.get() != sprite
                || lightingFacing != facing) {
            lightingPattern = pattern;
            lightingRevision = revision;
            this.lightingFingerprint = lightingFingerprint;
            lightingSprite = new WeakReference<>(sprite);
            lightingFacing = facing;
            worldLighting = capture.get();
            hasWorldLighting = true;
        }
        return worldLighting;
    }

    /**
     * Returns cached per-vertex lighting for the visual molten fill. This is
     * separate from the ceramic cache because the cavity surface has a
     * different topology, but it uses the same invalidation inputs.
     */
    synchronized SpecialWorldLighting.Result fillWorldLighting(
            MoldPattern pattern,
            long revision,
            long lightingFingerprint,
            TextureAtlasSprite sprite,
            Direction facing,
            Supplier<SpecialWorldLighting.Result> capture
    ) {
        if (!hasFillWorldLighting
                || !pattern.equals(fillLightingPattern)
                || revision != fillLightingRevision
                || lightingFingerprint != this.fillLightingFingerprint
                || fillLightingSprite.get() != sprite
                || fillLightingFacing != facing) {
            fillLightingPattern = pattern;
            fillLightingRevision = revision;
            this.fillLightingFingerprint = lightingFingerprint;
            fillLightingSprite = new WeakReference<>(sprite);
            fillLightingFacing = facing;
            fillWorldLighting = capture.get();
            hasFillWorldLighting = true;
        }
        return fillWorldLighting;
    }

    /** Drops cached arrays promptly when the client receives new mold state. */
    synchronized void invalidate() {
        carvingGuidePattern = MoldPattern.EMPTY;
        carvingGuideRevision = Long.MIN_VALUE;
        carvingGuide = MoldCarvingGuide.Layout.EMPTY;
        lightingPattern = MoldPattern.EMPTY;
        lightingRevision = Long.MIN_VALUE;
        lightingFingerprint = Long.MIN_VALUE;
        lightingSprite = new WeakReference<>(null);
        lightingFacing = null;
        worldLighting = null;
        hasWorldLighting = false;
        fillLightingPattern = MoldPattern.EMPTY;
        fillLightingRevision = Long.MIN_VALUE;
        fillLightingFingerprint = Long.MIN_VALUE;
        fillLightingSprite = new WeakReference<>(null);
        fillLightingFacing = null;
        fillWorldLighting = null;
        hasFillWorldLighting = false;
        fingerprintGameTime = Long.MIN_VALUE;
        cachedFingerprint = Long.MIN_VALUE;
    }
}
