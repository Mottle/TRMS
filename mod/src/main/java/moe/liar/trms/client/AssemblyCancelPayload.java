package moe.liar.trms.client;

import java.util.Objects;
import java.util.UUID;
import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

record AssemblyCancelPayload(UUID sessionId) implements CustomPacketPayload {
    static final Type<AssemblyCancelPayload> TYPE = new Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TrmsProtocol.NAMESPACE, "assembly_cancel"));
    static final StreamCodec<FriendlyByteBuf, AssemblyCancelPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AssemblyCancelPayload::sessionId, AssemblyCancelPayload::new);
    AssemblyCancelPayload { Objects.requireNonNull(sessionId, "sessionId"); }
    @Override public Type<AssemblyCancelPayload> type() { return TYPE; }
}
