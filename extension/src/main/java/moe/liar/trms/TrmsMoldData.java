package moe.liar.trms;

import java.util.Objects;
import java.util.Optional;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldPersistence;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The server-side data contract shared by a placed mold and its item form. */
final class TrmsMoldData {
    private TrmsMoldData() {
    }

    static void save(ValueOutput output, TrmsMoldPattern pattern, long revision,
                     Optional<MoldFillMaterial> fillMaterial) {
        output.putInt(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION);
        output.store(MoldPersistence.PATTERN_KEY, TrmsMoldPattern.CODEC, Objects.requireNonNull(pattern, "pattern"));
        output.putLong(MoldPersistence.REVISION_KEY, revision);
        Objects.requireNonNull(fillMaterial, "fillMaterial")
                .ifPresent(material -> output.putString(MoldPersistence.FILL_MATERIAL_KEY, material.id()));
    }

    static State load(ValueInput input) {
        int formatVersion = input.getIntOr(MoldPersistence.FORMAT_KEY, MoldPersistence.FORMAT_VERSION);
        if (formatVersion != MoldPersistence.FORMAT_VERSION) {
            throw new IllegalStateException("Unsupported TRMS mold data format: " + formatVersion);
        }
        TrmsMoldPattern pattern = input.read(MoldPersistence.PATTERN_KEY, TrmsMoldPattern.CODEC).orElseThrow(
                () -> new IllegalStateException("Missing or invalid TRMS mold Pattern data")
        );
        long revision = input.getLongOr(MoldPersistence.REVISION_KEY, 0L);
        if (revision < 0L) {
            throw new IllegalStateException("TRMS mold revision must not be negative");
        }
        Optional<MoldFillMaterial> fillMaterial = input.getString(MoldPersistence.FILL_MATERIAL_KEY)
                .map(MoldFillMaterial::of);
        if (pattern.isEmpty() && fillMaterial.isPresent()) {
            throw new IllegalStateException("An empty TRMS mold cannot contain fill material");
        }
        return new State(pattern, revision, fillMaterial);
    }

    static void storeItemPattern(ItemStack stack, DataComponentType<TrmsMoldPattern> component,
                                 TrmsMoldPattern pattern) {
        stack.set(component, Objects.requireNonNull(pattern, "pattern"));
    }

    static void storeItemPattern(DataComponentMap.Builder components,
                                 DataComponentType<TrmsMoldPattern> component,
                                 TrmsMoldPattern pattern) {
        components.set(component, Objects.requireNonNull(pattern, "pattern"));
    }

    static Optional<TrmsMoldPattern> readItemPattern(DataComponentGetter components,
                                                     DataComponentType<TrmsMoldPattern> component) {
        return Optional.ofNullable(components.get(component));
    }

    record State(TrmsMoldPattern pattern, long revision, Optional<MoldFillMaterial> fillMaterial) {
        State {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(fillMaterial, "fillMaterial");
        }
    }
}
