package moe.liar.trms.client;

import java.util.Objects;
import java.util.UUID;
import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

record AssemblyConfirmPayload(UUID sessionId, byte connectionX, byte connectionZ) implements CustomPacketPayload {
    static final Type<AssemblyConfirmPayload> TYPE = new Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TrmsProtocol.NAMESPACE, "assembly_confirm"));
    static final StreamCodec<FriendlyByteBuf, AssemblyConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AssemblyConfirmPayload::sessionId,
            ByteBufCodecs.BYTE, AssemblyConfirmPayload::connectionX,
            ByteBufCodecs.BYTE, AssemblyConfirmPayload::connectionZ,
            AssemblyConfirmPayload::new);

    AssemblyConfirmPayload { Objects.requireNonNull(sessionId, "sessionId"); }
    @Override public Type<AssemblyConfirmPayload> type() { return TYPE; }
}
