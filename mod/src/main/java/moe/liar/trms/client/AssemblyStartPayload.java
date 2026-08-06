package moe.liar.trms.client;

import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

record AssemblyStartPayload() implements CustomPacketPayload {
    static final Type<AssemblyStartPayload> TYPE = new Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TrmsProtocol.NAMESPACE, "assembly_start"));
    static final StreamCodec<FriendlyByteBuf, AssemblyStartPayload> STREAM_CODEC =
            StreamCodec.unit(new AssemblyStartPayload());

    @Override
    public Type<AssemblyStartPayload> type() { return TYPE; }
}
