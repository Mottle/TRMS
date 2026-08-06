package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsAssembledWeaponTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void componentAdapterRoundTripsTheCastingHandleAndAnchorInBothFormats() {
        TrmsAssembledWeapon weapon = new TrmsAssembledWeapon(
                TrmsMoldPattern.empty().carve(6, 6), MoldFillMaterial.GOLD,
                "minecraft:stick", 6, 7);

        TrmsAssembledWeapon persistent = TrmsAssembledWeapon.CODEC.parse(JsonOps.INSTANCE,
                TrmsAssembledWeapon.CODEC.encodeStart(JsonOps.INSTANCE, weapon)
                        .getOrThrow(error -> new AssertionError(error)))
                .getOrThrow(error -> new AssertionError(error));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        TrmsAssembledWeapon.STREAM_CODEC.encode(buffer, weapon);

        assertEquals(weapon, persistent);
        assertEquals(weapon, TrmsAssembledWeapon.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void componentAdapterRejectsAnAnchorThatIsNotBelowTheCasting() {
        assertThrows(IllegalArgumentException.class, () -> new TrmsAssembledWeapon(
                TrmsMoldPattern.empty().carve(6, 6), MoldFillMaterial.IRON,
                "minecraft:stick", 6, 6));
    }
}
