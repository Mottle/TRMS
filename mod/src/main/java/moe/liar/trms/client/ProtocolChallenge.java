package moe.liar.trms.client;

import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.netty.buffer.ByteBuf;

/** Server-to-client REQUIRED configuration challenge for the TRMS protocol handshake. */
public record ProtocolChallenge(UUID nonce, int expectedProtocol) implements CustomPacketPayload {
    public static final Type<ProtocolChallenge> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "protocol_challenge")
    );
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.LONG, UUID::getLeastSignificantBits,
            UUID::new
    );
    public static final StreamCodec<ByteBuf, ProtocolChallenge> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, ProtocolChallenge::nonce,
            ByteBufCodecs.VAR_INT, ProtocolChallenge::expectedProtocol,
            ProtocolChallenge::new
    );

    @Override
    public Type<ProtocolChallenge> type() {
        return TYPE;
    }
}
