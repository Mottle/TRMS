package moe.liar.trms;

import java.util.Objects;
import java.util.UUID;
import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-selected connection point for an active assembly session. */
record TrmsAssemblyConfirmPayload(UUID sessionId, byte connectionX, byte connectionZ)
        implements CustomPacketPayload {
    static final Type<TrmsAssemblyConfirmPayload> TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "assembly_confirm");
    static final StreamCodec<FriendlyByteBuf, TrmsAssemblyConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TrmsAssemblyConfirmPayload::sessionId,
            ByteBufCodecs.BYTE, TrmsAssemblyConfirmPayload::connectionX,
            ByteBufCodecs.BYTE, TrmsAssemblyConfirmPayload::connectionZ,
            TrmsAssemblyConfirmPayload::new);

    TrmsAssemblyConfirmPayload {
        Objects.requireNonNull(sessionId, "sessionId");
    }

    @Override
    public Type<TrmsAssemblyConfirmPayload> type() {
        return TYPE;
    }
}
