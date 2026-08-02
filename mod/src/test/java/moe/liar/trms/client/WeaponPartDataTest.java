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

class WeaponPartDataTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void itemComponentRoundTripsThePlayerShapedCastingInBothFormats() {
        WeaponPartData part = new WeaponPartData(
                MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow()
                        .predictCarve(7, 6).orElseThrow(),
                MoldFillMaterial.COPPER);

        WeaponPartData persistent = WeaponPartData.CODEC.parse(JsonOps.INSTANCE,
                WeaponPartData.CODEC.encodeStart(JsonOps.INSTANCE, part)
                        .getOrThrow(error -> new AssertionError(error)))
                .getOrThrow(error -> new AssertionError(error));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        WeaponPartData.STREAM_CODEC.encode(buffer, part);

        assertEquals(part, persistent);
        assertEquals(part, WeaponPartData.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void itemComponentRejectsAWeaponPartWithoutAnyCastingCells() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeaponPartData(MoldPattern.EMPTY, MoldFillMaterial.IRON));
    }
}
