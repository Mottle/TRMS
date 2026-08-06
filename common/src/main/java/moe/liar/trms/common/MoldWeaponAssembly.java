package moe.liar.trms.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Side-neutral rules for attaching one pixelated casting to a wooden handle. */
public final class MoldWeaponAssembly {
    public static final String STICK_MATERIAL = "minecraft:stick";
    /** The wood handle occupies the same one-pixel column as its connection point. */
    public static final int HANDLE_WIDTH = 1;
    public static final int HANDLE_THICKNESS = 1;
    public static final int HANDLE_LENGTH = 10;

    private MoldWeaponAssembly() {
    }

    /** A candidate is the empty pixel immediately below a carved pixel. */
    public record ConnectionPoint(int x, int z) {
        public ConnectionPoint {
            if (x < MoldPattern.INTERIOR_MIN || x > MoldPattern.INTERIOR_MAX
                    || z < MoldPattern.INTERIOR_MIN || z > MoldPattern.INTERIOR_MAX + 1) {
                throw new IllegalArgumentException("Connection point outside mold surface: " + x + "," + z);
            }
        }
    }

    /** Enumerates deterministic, duplicate-free connection points from top to bottom, left to right. */
    public static List<ConnectionPoint> legalConnectionPoints(MoldPattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        List<ConnectionPoint> points = new ArrayList<>();
        for (int z = MoldPattern.INTERIOR_MIN; z <= MoldPattern.INTERIOR_MAX; z++) {
            for (int x = MoldPattern.INTERIOR_MIN; x <= MoldPattern.INTERIOR_MAX; x++) {
                if (pattern.isCarved(x, z)) {
                    int below = z + 1;
                    if (!pattern.isCarved(x, below)) {
                        points.add(new ConnectionPoint(x, below));
                    }
                }
            }
        }
        return List.copyOf(points);
    }

    public static boolean isLegalConnection(MoldPattern pattern, int x, int z) {
        if (x < MoldPattern.INTERIOR_MIN || x > MoldPattern.INTERIOR_MAX
                || z < MoldPattern.INTERIOR_MIN || z > MoldPattern.INTERIOR_MAX + 1) {
            return false;
        }
        return legalConnectionPoints(pattern).contains(new ConnectionPoint(x, z));
    }
}
