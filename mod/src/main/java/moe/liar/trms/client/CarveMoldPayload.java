package moe.liar.trms.client;

import java.util.Objects;
import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** Client play-phase request for one preselected mold cell. */
public record CarveMoldPayload(
        BlockPos moldPos,
        byte cellX,
        byte cellZ,
        InteractionHand hand,
        long expectedRevision
) implements CustomPacketPayload {
    public static final Type<CarveMoldPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TrmsProtocol.NAMESPACE, "carve_mold")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CarveMoldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    CarveMoldPayload::moldPos,
                    ByteBufCodecs.BYTE,
                    CarveMoldPayload::cellX,
                    ByteBufCodecs.BYTE,
                    CarveMoldPayload::cellZ,
                    InteractionHand.STREAM_CODEC,
                    CarveMoldPayload::hand,
                    ByteBufCodecs.VAR_LONG,
                    CarveMoldPayload::expectedRevision,
                    CarveMoldPayload::new
            );

    public CarveMoldPayload {
        moldPos = Objects.requireNonNull(moldPos, "moldPos").immutable();
        hand = Objects.requireNonNull(hand, "hand");
    }

    @Override
    public Type<CarveMoldPayload> type() {
        return TYPE;
    }
}
