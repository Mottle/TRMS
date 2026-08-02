package moe.liar.trms.client;

import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.netty.buffer.ByteBuf;

/** Client-to-server REQUIRED configuration response for the TRMS protocol handshake. */
public record ProtocolResponse(UUID nonce, int clientProtocol) implements CustomPacketPayload {
    public static final Type<ProtocolResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "protocol_response")
    );
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.LONG, UUID::getLeastSignificantBits,
            UUID::new
    );
    public static final StreamCodec<ByteBuf, ProtocolResponse> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, ProtocolResponse::nonce,
            ByteBufCodecs.VAR_INT, ProtocolResponse::clientProtocol,
            ProtocolResponse::new
    );

    @Override
    public Type<ProtocolResponse> type() {
        return TYPE;
    }
}
