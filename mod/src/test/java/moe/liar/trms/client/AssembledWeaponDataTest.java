package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AssembledWeaponDataTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void itemComponentRoundTripsTheCastingHandleAndAnchorInBothFormats() {
        AssembledWeaponData weapon = new AssembledWeaponData(
                MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow(), MoldFillMaterial.COPPER,
                "minecraft:stick", 6, 7);

        AssembledWeaponData persistent = AssembledWeaponData.CODEC.parse(JsonOps.INSTANCE,
                AssembledWeaponData.CODEC.encodeStart(JsonOps.INSTANCE, weapon)
                        .getOrThrow(error -> new AssertionError(error)))
                .getOrThrow(error -> new AssertionError(error));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        AssembledWeaponData.STREAM_CODEC.encode(buffer, weapon);

        assertEquals(weapon, persistent);
        assertEquals(weapon, AssembledWeaponData.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void itemComponentRejectsAnAnchorThatIsNotBelowTheCasting() {
        assertThrows(IllegalArgumentException.class, () -> new AssembledWeaponData(
                MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow(), MoldFillMaterial.IRON,
                "minecraft:stick", 6, 6));
    }
}
