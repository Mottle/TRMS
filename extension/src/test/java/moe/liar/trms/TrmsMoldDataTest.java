package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.Optional;
import moe.liar.trms.common.MoldCooling;
import moe.liar.trms.common.MoldFillMaterial;
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
        CompoundTag envelope = saveEnvelope(pattern, 3L, Optional.empty());
        TrmsMoldData.State loaded = TrmsMoldData.load(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY, envelope));
        assertEquals(MoldPersistence.FORMAT_VERSION, envelope.getIntOr(MoldPersistence.FORMAT_KEY, -1));
        assertTrue(envelope.contains(MoldPersistence.PATTERN_KEY));
        assertEquals(pattern, loaded.pattern());
        assertEquals(3L, loaded.revision());
        assertTrue(loaded.fillMaterial().isEmpty());
        assertEquals(0, loaded.coolingStage());

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
        CompoundTag valid = saveEnvelope(
                TrmsMoldPattern.empty().carve(3, 3), 1L, Optional.empty());

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
    void placedFillMaterialSurvivesPersistenceButIsNotAnItemComponent() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(6, 6).carve(7, 6);
        CompoundTag envelope = saveEnvelope(pattern, 9L, Optional.of(MoldFillMaterial.COPPER), 4);

        TrmsMoldData.State loaded = TrmsMoldData.load(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY, envelope));

        assertEquals(MoldFillMaterial.COPPER, loaded.fillMaterial().orElseThrow());
        assertEquals(4, loaded.coolingStage());
        assertEquals(MoldFillMaterial.COPPER.id(),
                envelope.getStringOr(MoldPersistence.FILL_MATERIAL_KEY, ""));

        DataComponentMap.Builder droppedComponents = DataComponentMap.builder();
        TrmsMoldData.storeItemPattern(droppedComponents, testPatternComponent, loaded.pattern());
        DataComponentMap dropped = droppedComponents.build();
        assertEquals(pattern, dropped.get(testPatternComponent));
    }

    @Test
    void persistenceSupportsExtensibleMaterialIdsAndRejectsFilledEmptyMolds() {
        MoldFillMaterial customMaterial = MoldFillMaterial.of("example:bronze");
        CompoundTag customEnvelope = saveEnvelope(
                TrmsMoldPattern.empty().carve(4, 4), 2L, Optional.of(customMaterial));
        TrmsMoldData.State customLoaded = TrmsMoldData.load(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY, customEnvelope));
        assertEquals(customMaterial, customLoaded.fillMaterial().orElseThrow());

        CompoundTag impossibleEmptyFill = saveEnvelope(
                TrmsMoldPattern.empty(), 1L, Optional.empty());
        impossibleEmptyFill.putString(MoldPersistence.FILL_MATERIAL_KEY, MoldFillMaterial.IRON.id());
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, impossibleEmptyFill)));
    }

    @Test
    void persistenceRejectsCoolingOutsideTheFilledVisibleStageRange() {
        CompoundTag validFilled = saveEnvelope(
                TrmsMoldPattern.empty().carve(4, 4), 2L, Optional.of(MoldFillMaterial.IRON), 0);
        CompoundTag invalidFilledStage = validFilled.copy();
        invalidFilledStage.putInt(MoldPersistence.COOLING_STAGE_KEY, MoldCooling.STAGE_COUNT);
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, invalidFilledStage)));

        CompoundTag invalidUnfilledStage = saveEnvelope(
                TrmsMoldPattern.empty().carve(4, 4), 2L, Optional.empty(), 0);
        invalidUnfilledStage.putInt(MoldPersistence.COOLING_STAGE_KEY, 1);
        assertThrows(IllegalStateException.class, () -> TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, invalidUnfilledStage)));
    }

    @Test
    void savingRejectsInvalidStateBeforeItCanProduceAnUnreadableWorldEnvelope() {
        TrmsMoldPattern nonEmpty = TrmsMoldPattern.empty().carve(4, 4);

        assertThrows(IllegalArgumentException.class, () -> TrmsMoldData.save(
                TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING), nonEmpty, -1L,
                Optional.empty(), 0));
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldData.save(
                TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING), TrmsMoldPattern.empty(), 0L,
                Optional.of(MoldFillMaterial.COPPER), 0));
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldData.save(
                TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING), nonEmpty, 0L,
                Optional.of(MoldFillMaterial.COPPER), MoldCooling.STAGE_COUNT));
        assertThrows(IllegalArgumentException.class, () -> TrmsMoldData.save(
                TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING), nonEmpty, 0L,
                Optional.empty(), 1));
    }

    @Test
    void savingTheRealValueIoEnvelopeUsesTheProductionCodec() {
        TrmsMoldPattern pattern = TrmsMoldPattern.empty().carve(10, 10);
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TrmsMoldData.save(output, pattern, 12L, Optional.of(MoldFillMaterial.IRON), 7);
        CompoundTag tag = output.buildResult();

        TrmsMoldData.State decoded = TrmsMoldData.load(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
        assertEquals(pattern, decoded.pattern());
        assertEquals(12L, decoded.revision());
        assertEquals(MoldFillMaterial.IRON, decoded.fillMaterial().orElseThrow());
        assertEquals(7, decoded.coolingStage());
    }

    private static CompoundTag saveEnvelope(TrmsMoldPattern pattern, long revision,
                                            Optional<MoldFillMaterial> fillMaterial) {
        return saveEnvelope(pattern, revision, fillMaterial, 0);
    }

    private static CompoundTag saveEnvelope(TrmsMoldPattern pattern, long revision,
                                            Optional<MoldFillMaterial> fillMaterial, int coolingStage) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        TrmsMoldData.save(output, pattern, revision, fillMaterial, coolingStage);
        return output.buildResult();
    }
}
