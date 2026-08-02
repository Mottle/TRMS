package moe.liar.trms.client;

import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldCooling;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/** Client-only presentation attributes for the visual mold-fill materials. */
final class MoldFillVisual {
    /** Saturated red copper, kept fully opaque so the animated molten atlas remains legible. */
    static final int COPPER_COLOR = 0xFFC06C43;
    /** Cool silver-gray tint for the initial iron demonstration material. */
    static final int IRON_COLOR = 0xFFD5D9E1;
    /** Warm gold tint for the cooled gold weapon part. */
    static final int GOLD_COLOR = 0xFFFFC857;
    /** High-luminance yellow-gold tint visible only while gold remains molten in a mold. */
    static final int MOLTEN_GOLD_COLOR = 0xFFFFE06A;
    /** Deliberately conspicuous fallback for a valid material missing client presentation support. */
    static final int UNKNOWN_COLOR = 0xFFFF00FF;
    private static final MoldFillVisual COPPER = new MoldFillVisual(COPPER_COLOR);
    private static final MoldFillVisual IRON = new MoldFillVisual(IRON_COLOR);
    private static final MoldFillVisual GOLD = new MoldFillVisual(MOLTEN_GOLD_COLOR, GOLD_COLOR);
    private static final MoldFillVisual UNKNOWN = new MoldFillVisual(UNKNOWN_COLOR);

    private final int moltenColor;
    private final int solidColor;

    private MoldFillVisual(int color) {
        this(color, color);
    }

    private MoldFillVisual(int moltenColor, int solidColor) {
        this.moltenColor = moltenColor;
        this.solidColor = solidColor;
    }

    static @Nullable MoldFillVisual forMaterial(@Nullable MoldFillMaterial material) {
        if (material == null) {
            return null;
        }
        return switch (material.id()) {
            case "trms:copper" -> COPPER;
            case "trms:iron" -> IRON;
            case "trms:gold" -> GOLD;
            // Keep a server-authoritative, future material visible rather than
            // silently making a filled mold look empty on an older client.
            default -> UNKNOWN;
        };
    }

    int colorForCoolingTicks(int elapsedTicks) {
        float brightness = MoldCooling.brightnessForElapsedTicks(elapsedTicks);
        return ARGB.color(ARGB.alpha(moltenColor),
                Math.round(ARGB.red(moltenColor) * brightness),
                Math.round(ARGB.green(moltenColor) * brightness),
                Math.round(ARGB.blue(moltenColor) * brightness));
    }

    int baseColor() {
        return solidColor;
    }
}
