package moe.liar.trms;

import java.util.Objects;
import java.util.UUID;
import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client request to close an assembly preview without consuming inputs. */
record TrmsAssemblyCancelPayload(UUID sessionId) implements CustomPacketPayload {
    static final Type<TrmsAssemblyCancelPayload> TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "assembly_cancel");
    static final StreamCodec<FriendlyByteBuf, TrmsAssemblyCancelPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TrmsAssemblyCancelPayload::sessionId,
            TrmsAssemblyCancelPayload::new);

    TrmsAssemblyCancelPayload {
        Objects.requireNonNull(sessionId, "sessionId");
    }

    @Override
    public Type<TrmsAssemblyCancelPayload> type() {
        return TYPE;
    }
}
