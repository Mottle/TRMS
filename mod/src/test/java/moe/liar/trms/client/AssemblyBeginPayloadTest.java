package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AssemblyBeginPayloadTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsTheServerPreviewAndItsLegalPoints() {
        AssemblyBeginPayload sent = new AssemblyBeginPayload(
                UUID.randomUUID(), MoldPattern.EMPTY.predictCarve(6, 6).orElseThrow(),
                MoldFillMaterial.COPPER,
                List.of(new AssemblyBeginPayload.ConnectionPoint((byte) 6, (byte) 7)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);

        AssemblyBeginPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, AssemblyBeginPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(AssemblyBeginPayload.TYPE, sent.type());
    }
}
