package moe.liar.trms.client;

import moe.liar.trms.common.MoldFillMaterial;
import org.jspecify.annotations.Nullable;

/** Client-only presentation attributes for the visual mold-fill materials. */
final class MoldFillVisual {
    /** Saturated red copper, kept fully opaque so the animated molten atlas remains legible. */
    static final int COPPER_COLOR = 0xFFC06C43;
    /** Cool silver-gray tint for the initial iron demonstration material. */
    static final int IRON_COLOR = 0xFFD5D9E1;
    /** Deliberately conspicuous fallback for a valid material missing client presentation support. */
    static final int UNKNOWN_COLOR = 0xFFFF00FF;
    private static final MoldFillVisual COPPER = new MoldFillVisual(COPPER_COLOR);
    private static final MoldFillVisual IRON = new MoldFillVisual(IRON_COLOR);
    private static final MoldFillVisual UNKNOWN = new MoldFillVisual(UNKNOWN_COLOR);

    private final int color;

    private MoldFillVisual(int color) {
        this.color = color;
    }

    static @Nullable MoldFillVisual forMaterial(@Nullable MoldFillMaterial material) {
        if (material == null) {
            return null;
        }
        return switch (material.id()) {
            case "trms:copper" -> COPPER;
            case "trms:iron" -> IRON;
            // Keep a server-authoritative, future material visible rather than
            // silently making a filled mold look empty on an older client.
            default -> UNKNOWN;
        };
    }

    int color() {
        return color;
    }
}
