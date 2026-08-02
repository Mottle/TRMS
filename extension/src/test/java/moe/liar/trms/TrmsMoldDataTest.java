package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import moe.liar.trms.common.MoldPersistence;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsMoldDataTest {
    private static DataComponentType<TrmsMoldPattern> testPatternComponent;

    @BeforeAll
    static void initializeMinecraftForValueIoAndComponents() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testPatternComponent = DataComponentType.<TrmsMoldPattern>builder()
                .persistent(TrmsMoldPattern.CODEC)
                .networkSynchronized(TrmsMoldPattern.STREAM_CODEC)
                .build();
    }

    @Test
    void patternSurvivesBlockEntityPersistenceAndItemComponentPlacement() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty()
                .carve(7, 7)
                .carve(8, 8)
                .carve(9, 8);
        CompoundTag envelope = saveEnvelope(pattern, 3L);
        TrmsMoldData.State loaded = TrmsMoldData.load(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY, envelope));
        assertEquals(MoldPersistence.FORMAT_VERSION, envelope.getIntOr(MoldPersistence.FORMAT_KEY, -1));
        assertTrue(envelope.contains(MoldPersistence.PATTERN_KEY));
        assertEquals(pattern, loaded.pattern());
        assertEquals(3L, loaded.revision());

        DataComponentMap.Builder droppedComponents = DataComponentMap.builder();
        TrmsMoldData.storeItemPattern(droppedComponents, testPatternComponent, loaded.pattern());
        DataComponentMap dropped = droppedComponents.build();
        assertEquals(pattern, TrmsMoldData.readItemPattern(dropped, testPatternComponent).orElseThrow());

        TrmsMoldPattern restored = TrmsMoldData.readItemPattern(dropped, testPatternComponent).orElseThrow();
        assertEquals(pattern, restored);
    }

    @Test
    void itemComponentUsesTheSameVersionedPatternCodec() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(2, 2);
        DataComponentMap.Builder components = DataComponentMap.builder();
        TrmsMoldData.storeItemPattern(components, testPatternComponent, pattern);
        DataComponentMap stackComponents = components.build();

        assertEquals(pattern, testPatternComponent.codec().encodeStart(JsonOps.INSTANCE, stackComponents.get(testPatternComponent))
                .flatMap(encoded -> testPatternComponent.codec().parse(JsonOps.INSTANCE, encoded))
                .getOrThrow(error -> new AssertionError(error)));
    }

    @Test
    void persistenceRejectsMissingPatternUnsupportedFormatAndNegativeRevision() {
        CompoundTag valid = saveEnvelope(TrmsMoldPattern.empty().carve(3, 3), 1L);

        CompoundTag missingPattern = valid.copy();
        missingPattern.remove(MoldPersistence.PATTERN_KEY);
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, missingPattern)));

        CompoundTag unsupportedFormat = valid.copy();
        unsupportedFormat.putInt(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION + 1);
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, unsupportedFormat)));

        CompoundTag negativeRevision = valid.copy();
        negativeRevision.putLong(MoldPersistence.REVISION_KEY, -1L);
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, negativeRevision)));
    }

    @Test
    void savingTheRealValueIoEnvelopeUsesTheProductionCodec() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(10, 10);
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TrmsMoldData.save(output, pattern, 12L);
        CompoundTag tag = output.buildResult();

        TrmsMoldData.State decoded = TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
        assertEquals(pattern, decoded.pattern());
        assertEquals(12L, decoded.revision());
    }

    private static CompoundTag saveEnvelope(TrmsMoldPattern pattern, long revision) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TrmsMoldData.save(output, pattern, revision);
        return output.buildResult();
    }
}
