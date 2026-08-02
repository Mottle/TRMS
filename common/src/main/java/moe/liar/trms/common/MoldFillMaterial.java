package moe.liar.trms.common;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable, side-neutral identity of material visually occupying a mold.
 *
 * <p>A fill material is deliberately only a stable namespaced ID. Runtime
 * endpoints attach their own item, rendering, and presentation behavior to
 * that ID without introducing Minecraft or NeoForge dependencies here.</p>
 */
public record MoldFillMaterial(String id) {
    /** Keeps malformed persistent or item-component material IDs bounded before they reach a network codec. */
    public static final int MAX_ID_LENGTH = 256;
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public static final MoldFillMaterial COPPER = new MoldFillMaterial(TrmsProtocol.NAMESPACE + ":copper");
    public static final MoldFillMaterial IRON = new MoldFillMaterial(TrmsProtocol.NAMESPACE + ":iron");

    public MoldFillMaterial {
        Objects.requireNonNull(id, "id");
        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("TRMS mold fill material ID exceeds " + MAX_ID_LENGTH + " characters");
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid namespaced mold fill material ID: " + id);
        }
    }

    /** Creates a validated material identity from its persistent string form. */
    public static MoldFillMaterial of(String id) {
        return new MoldFillMaterial(id);
    }

    /** Returns the portion before the namespace separator. */
    public String namespace() {
        return id.substring(0, id.indexOf(':'));
    }

    /** Returns the portion after the namespace separator. */
    public String path() {
        return id.substring(id.indexOf(':') + 1);
    }

    @Override
    public String toString() {
        return id;
    }
}
