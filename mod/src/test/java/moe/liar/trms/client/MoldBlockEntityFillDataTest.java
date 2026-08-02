package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldPersistence;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;

class MoldBlockEntityFillDataTest {
    @Test
    void fillMaterialIdRoundTripsThroughTheNormalBlockEntityValueEnvelope() {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        MoldBlockEntity.writeFillMaterial(output, MoldFillMaterial.COPPER);
        CompoundTag tag = output.buildResult();

        assertEquals(Optional.of(MoldFillMaterial.COPPER.id()), tag.getString(MoldPersistence.FILL_MATERIAL_KEY));
        assertEquals(MoldFillMaterial.COPPER,
                MoldBlockEntity.readFillMaterial(TagValueInput.create(
                        ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag)));
    }

    @Test
    void absentFillMaterialMeansUnfilledAndDoesNotWriteAPlaceholderKey() {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        MoldBlockEntity.writeFillMaterial(output, null);
        CompoundTag tag = output.buildResult();

        assertEquals(Optional.empty(), tag.getString(MoldPersistence.FILL_MATERIAL_KEY));
        assertNull(MoldBlockEntity.readFillMaterial(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag)));
    }

    @Test
    void clientRejectsMalformedAndImpossibleServerFillState() {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.putString(MoldPersistence.FILL_MATERIAL_KEY, " ");
        CompoundTag malformed = output.buildResult();

        assertThrows(IllegalArgumentException.class, () -> MoldBlockEntity.readFillMaterial(
                TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, malformed)));
        assertThrows(IllegalStateException.class,
                () -> MoldBlockEntity.validateFillState(MoldPattern.EMPTY, MoldFillMaterial.IRON, 0));
        assertDoesNotThrow(() -> MoldBlockEntity.validateFillState(
                MoldPattern.EMPTY.predictCarve(4, 4).orElseThrow(), MoldFillMaterial.IRON, 4));
        assertThrows(IllegalStateException.class, () -> MoldBlockEntity.validateFillState(
                MoldPattern.EMPTY.predictCarve(4, 4).orElseThrow(), MoldFillMaterial.IRON, 10));
        assertThrows(IllegalStateException.class, () -> MoldBlockEntity.validateFillState(
                MoldPattern.EMPTY.predictCarve(4, 4).orElseThrow(), null, 1));
    }
}
