package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsWeaponAssemblyPayloadTest {
    @BeforeAll
    static void initializeGameVersionBeforeCreatingNetworkBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsConfirmSelection() {
        TrmsAssemblyConfirmPayload sent = new TrmsAssemblyConfirmPayload(
                UUID.randomUUID(), (byte) 6, (byte) 15);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        TrmsAssemblyConfirmPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, TrmsAssemblyConfirmPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(TrmsAssemblyConfirmPayload.TYPE, sent.type());
    }

    @Test
    void roundTripsCancellation() {
        TrmsAssemblyCancelPayload sent = new TrmsAssemblyCancelPayload(UUID.randomUUID());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        TrmsAssemblyCancelPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, TrmsAssemblyCancelPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(TrmsAssemblyCancelPayload.TYPE, sent.type());
    }

    @Test
    void roundTripsTheServerPreviewAndItsLegalPoints() {
        TrmsAssemblyBeginPayload sent = new TrmsAssemblyBeginPayload(
                UUID.randomUUID(), TrmsMoldPattern.empty().carve(6, 6),
                moe.liar.trms.common.MoldFillMaterial.COPPER,
                List.of(new TrmsAssemblyBeginPayload.TrmsAssemblyPoint((byte) 6, (byte) 7)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        TrmsAssemblyBeginPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, TrmsAssemblyBeginPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
        assertSame(TrmsAssemblyBeginPayload.TYPE, sent.type());
    }

    @Test
    void rejectsDuplicateOrOutOfRangePreviewPoints() {
        var point = new TrmsAssemblyBeginPayload.TrmsAssemblyPoint((byte) 6, (byte) 7);
        assertThrows(IllegalArgumentException.class, () -> new TrmsAssemblyBeginPayload(
                UUID.randomUUID(), TrmsMoldPattern.empty().carve(6, 6),
                moe.liar.trms.common.MoldFillMaterial.COPPER, List.of(point, point)));
        assertThrows(IllegalArgumentException.class,
                () -> new TrmsAssemblyBeginPayload.TrmsAssemblyPoint((byte) 0, (byte) 7));
    }
}
