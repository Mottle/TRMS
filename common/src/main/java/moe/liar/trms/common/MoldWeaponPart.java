package moe.liar.trms.common;

import java.util.Objects;

/**
 * The side-neutral result of one completed single-layer Mold casting.
 *
 * <p>Runtime endpoints serialize this value in their own Minecraft data
 * component adapters.  Keeping the identity here makes a weapon part's
 * material and player-authored silhouette available without coupling common
 * code to a game registry or renderer.</p>
 */
public record MoldWeaponPart(MoldPattern pattern, MoldFillMaterial material) {
    public MoldWeaponPart {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(material, "material");
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("A TRMS weapon part requires a non-empty mold pattern");
        }
    }
}
