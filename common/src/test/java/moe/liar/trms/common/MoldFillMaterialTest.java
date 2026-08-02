package moe.liar.trms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for stable, side-neutral mold fill identities. */
class MoldFillMaterialTest {
    @Test
    void exposesStableInitialMaterialIds() {
        assertEquals("trms:copper", MoldFillMaterial.COPPER.id());
        assertEquals("trms:iron", MoldFillMaterial.IRON.id());
        assertEquals("trms", MoldFillMaterial.COPPER.namespace());
        assertEquals("copper", MoldFillMaterial.COPPER.path());
        assertEquals("trms:copper", MoldFillMaterial.COPPER.toString());
    }

    @Test
    void acceptsExtensibleMinecraftStyleNamespacedIds() {
        MoldFillMaterial alloy = MoldFillMaterial.of("example_metals:alloys/red-bronze");

        assertEquals("example_metals", alloy.namespace());
        assertEquals("alloys/red-bronze", alloy.path());
        assertEquals(alloy, MoldFillMaterial.of(alloy.id()));
        assertNotEquals(alloy, MoldFillMaterial.COPPER);
    }

    @Test
    void rejectsMissingNamespaceUppercaseAndMalformedIds() {
        assertThrows(NullPointerException.class, () -> MoldFillMaterial.of(null));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("copper"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("TRMS:copper"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("trms:Copper"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of(":copper"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("trms:"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("trms:copper ingot"));
        assertThrows(IllegalArgumentException.class, () -> MoldFillMaterial.of("trms:copper:hot"));
    }

    @Test
    void addsFillPersistenceWithoutChangingProtocolVersion() {
        assertEquals("FillMaterial", MoldPersistence.FILL_MATERIAL_KEY);
        assertEquals(1, TrmsProtocol.VERSION);
        assertEquals("trms-handshake-1", TrmsProtocol.HANDSHAKE_TRANSPORT_VERSION);
        assertEquals("trms-carving-1", TrmsProtocol.CARVING_TRANSPORT_VERSION);
    }
}
