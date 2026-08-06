package moe.liar.trms;

import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client request to begin the dedicated weapon assembly preview. */
record TrmsAssemblyStartPayload() implements CustomPacketPayload {
    static final Type<TrmsAssemblyStartPayload> TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "assembly_start");
    static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, TrmsAssemblyStartPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.unit(new TrmsAssemblyStartPayload());

    @Override
    public Type<TrmsAssemblyStartPayload> type() {
        return TYPE;
    }
}
