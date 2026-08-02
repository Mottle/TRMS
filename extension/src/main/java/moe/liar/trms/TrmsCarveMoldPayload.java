package moe.liar.trms;

import java.util.Objects;
import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Client-selected mold cell sent during play. The Extension independently
 * validates every value before it changes a mold or damages a tool.
 */
record TrmsCarveMoldPayload(
        BlockPos moldPos,
        byte cellX,
        byte cellZ,
        InteractionHand hand,
        long expectedRevision
) implements CustomPacketPayload {
    static final Type<TrmsCarveMoldPayload> TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "carve_mold");
    static final StreamCodec<FriendlyByteBuf, TrmsCarveMoldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    TrmsCarveMoldPayload::moldPos,
                    ByteBufCodecs.BYTE,
                    TrmsCarveMoldPayload::cellX,
                    ByteBufCodecs.BYTE,
                    TrmsCarveMoldPayload::cellZ,
                    InteractionHand.STREAM_CODEC,
                    TrmsCarveMoldPayload::hand,
                    ByteBufCodecs.VAR_LONG,
                    TrmsCarveMoldPayload::expectedRevision,
                    TrmsCarveMoldPayload::new
            );

    TrmsCarveMoldPayload {
        moldPos = Objects.requireNonNull(moldPos, "moldPos").immutable();
        hand = Objects.requireNonNull(hand, "hand");
    }

    @Override
    public Type<TrmsCarveMoldPayload> type() {
        return TYPE;
    }
}
