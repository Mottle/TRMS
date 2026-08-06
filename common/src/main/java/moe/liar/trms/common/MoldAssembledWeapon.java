package moe.liar.trms.common;

import java.util.Objects;

/** Immutable, side-neutral data carried by a completed weapon item. */
public record MoldAssembledWeapon(
        MoldPattern pattern,
        MoldFillMaterial material,
        String handleMaterial,
        MoldWeaponAssembly.ConnectionPoint connectionPoint
) {
    public MoldAssembledWeapon {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(handleMaterial, "handleMaterial");
        Objects.requireNonNull(connectionPoint, "connectionPoint");
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("An assembled weapon requires a non-empty pattern");
        }
        MoldFillMaterial.of(handleMaterial);
        if (!MoldWeaponAssembly.isLegalConnection(pattern, connectionPoint.x(), connectionPoint.z())) {
            throw new IllegalArgumentException("Connection point is not below the casting outline");
        }
    }
}
