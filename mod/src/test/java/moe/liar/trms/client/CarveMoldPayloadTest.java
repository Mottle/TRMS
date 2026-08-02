package moe.liar.trms.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CarveMoldPayloadTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsTheSelectedCellHandAndRevision() {
        CarveMoldPayload sent = new CarveMoldPayload(
                new BlockPos(-17, 72, 43), (byte) 14, (byte) 1,
                InteractionHand.OFF_HAND, 97L
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);

        CarveMoldPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, CarveMoldPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(CarveMoldPayload.TYPE, sent.type());
    }
}
