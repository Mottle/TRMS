package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsCarveMoldPayloadTest {
    @BeforeAll
    static void initializeGameVersionBeforeCreatingNetworkBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsTheClientSelectedCellHandAndRevision() {
        TrmsCarveMoldPayload sent = new TrmsCarveMoldPayload(
                new BlockPos(-17, 72, 43), (byte) 14, (byte) 1,
                InteractionHand.OFF_HAND, 97L
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        TrmsCarveMoldPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, TrmsCarveMoldPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(TrmsCarveMoldPayload.TYPE, sent.type());
    }
}
