package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class MoldRenderCacheTest {
    @Test
    void retainsGuideLayoutAcrossShortLivedRenderStatesUntilMoldStateChanges() {
        MoldRenderCache cache = new MoldRenderCache();

        MoldCarvingGuide.Layout first = cache.carvingGuide(MoldPattern.EMPTY, 0L);
        MoldCarvingGuide.Layout second = cache.carvingGuide(MoldPattern.EMPTY, 0L);
        MoldCarvingGuide.Layout afterRevision = cache.carvingGuide(MoldPattern.EMPTY, 1L);
        MoldPattern carved = MoldPattern.EMPTY.predictCarve(8, 8).orElseThrow();
        MoldCarvingGuide.Layout afterPattern = cache.carvingGuide(carved, 1L);

        assertSame(first, second, "unchanged render extractions must reuse the same guide layout");
        assertNotSame(first, afterRevision, "a new authoritative revision must rebuild the guide layout");
        assertNotSame(afterRevision, afterPattern, "pattern equality remains part of the cache key");
    }

    @Test
    void retainsLightingUntilOneOfItsRenderInputsChanges() {
        MoldRenderCache cache = new MoldRenderCache();
        AtomicInteger captures = new AtomicInteger();
        Supplier<MoldMeshBuilder.WorldLighting> capture = () -> {
            captures.incrementAndGet();
            return null;
        };

        cache.worldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, capture);
        cache.worldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, capture);
        cache.worldLighting(MoldPattern.EMPTY, 0L, 43, null, Direction.SOUTH, capture);
        cache.worldLighting(MoldPattern.EMPTY, 0L, 43, null, Direction.WEST, capture);
        cache.worldLighting(MoldPattern.EMPTY, 1L, 43, null, Direction.WEST, capture);

        assertEquals(4, captures.get(),
                "only the unchanged second extraction may reuse the sampled lighting");
    }

    @Test
    void explicitInvalidationReleasesEveryDerivedValueIncludingMoltenFillLighting() {
        MoldRenderCache cache = new MoldRenderCache();
        MoldCarvingGuide.Layout first = cache.carvingGuide(MoldPattern.EMPTY, 0L);
        AtomicInteger captures = new AtomicInteger();

        cache.worldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, () -> {
            captures.incrementAndGet();
            return null;
        });
        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, () -> {
            captures.incrementAndGet();
            return null;
        });
        cache.invalidate();

        assertNotSame(first, cache.carvingGuide(MoldPattern.EMPTY, 0L));
        cache.worldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, () -> {
            captures.incrementAndGet();
            return null;
        });
        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, () -> {
            captures.incrementAndGet();
            return null;
        });
        assertEquals(4, captures.get(),
                "a neighbour-shape invalidation must refresh both ceramic and molten fill lighting");
    }

    @Test
    void fillLightingUsesTheSameBoundedInvalidationRulesAsCeramicLighting() {
        MoldRenderCache cache = new MoldRenderCache();
        AtomicInteger captures = new AtomicInteger();
        Supplier<MoldMeshBuilder.WorldLighting> capture = () -> {
            captures.incrementAndGet();
            return null;
        };

        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, capture);
        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 42, null, Direction.SOUTH, capture);
        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 43, null, Direction.SOUTH, capture);
        cache.fillWorldLighting(MoldPattern.EMPTY, 0L, 43, null, Direction.WEST, capture);
        cache.fillWorldLighting(MoldPattern.EMPTY, 1L, 43, null, Direction.WEST, capture);

        assertEquals(4, captures.get(),
                "fill lighting must only be recaptured when its render inputs change");
    }
}
