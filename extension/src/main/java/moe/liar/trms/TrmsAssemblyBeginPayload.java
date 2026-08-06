package moe.liar.trms;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.TrmsProtocol;
import moe.liar.horizon.extension.network.PayloadTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-authoritative preview data for one assembly session. */
record TrmsAssemblyBeginPayload(
        UUID sessionId,
        TrmsMoldPattern pattern,
        MoldFillMaterial material,
        List<TrmsAssemblyPoint> legalPoints
) implements CustomPacketPayload {
    static final Type<TrmsAssemblyBeginPayload> TYPE =
            PayloadTypes.create(TrmsProtocol.NAMESPACE, "assembly_begin");
    static final StreamCodec<RegistryFriendlyByteBuf, TrmsAssemblyBeginPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TrmsAssemblyBeginPayload value) {
            UUIDUtil.STREAM_CODEC.encode(buffer, value.sessionId());
            TrmsMoldPattern.STREAM_CODEC.encode(buffer, value.pattern());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.material().id());
            buffer.writeVarInt(value.legalPoints().size());
            for (TrmsAssemblyPoint point : value.legalPoints()) {
                buffer.writeByte(point.x());
                buffer.writeByte(point.z());
            }
        }

        @Override
        public TrmsAssemblyBeginPayload decode(RegistryFriendlyByteBuf buffer) {
            UUID session = UUIDUtil.STREAM_CODEC.decode(buffer);
            TrmsMoldPattern pattern = TrmsMoldPattern.STREAM_CODEC.decode(buffer);
            MoldFillMaterial material = MoldFillMaterial.of(ByteBufCodecs.STRING_UTF8.decode(buffer));
            int count = buffer.readVarInt();
            if (count < 0 || count > 196) {
                throw new IllegalArgumentException("Invalid assembly connection-point count: " + count);
            }
            java.util.ArrayList<TrmsAssemblyPoint> points = new java.util.ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                points.add(new TrmsAssemblyPoint(buffer.readByte(), buffer.readByte()));
            }
            return new TrmsAssemblyBeginPayload(session, pattern, material, List.copyOf(points));
        }
    };

    TrmsAssemblyBeginPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(material, "material");
        legalPoints = List.copyOf(Objects.requireNonNull(legalPoints, "legalPoints"));
        if (legalPoints.isEmpty() || legalPoints.size() > 196) {
            throw new IllegalArgumentException("An assembly preview requires 1..196 legal points");
        }
        if (new HashSet<>(legalPoints).size() != legalPoints.size()) {
            throw new IllegalArgumentException("Assembly preview contains duplicate connection points");
        }
    }

    @Override
    public Type<TrmsAssemblyBeginPayload> type() {
        return TYPE;
    }

    record TrmsAssemblyPoint(byte x, byte z) {
        TrmsAssemblyPoint {
            if (x < 1 || x > 14 || z < 1 || z > 15) {
                throw new IllegalArgumentException("Assembly point outside mold surface: " + x + "," + z);
            }
        }
    }
}
