package moe.liar.trms.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.UUID;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.TrmsProtocol;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

record AssemblyBeginPayload(UUID sessionId, MoldPattern pattern, MoldFillMaterial material,
                            List<ConnectionPoint> legalPoints) implements CustomPacketPayload {
    static final Type<AssemblyBeginPayload> TYPE = new Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TrmsProtocol.NAMESPACE, "assembly_begin"));
    static final StreamCodec<RegistryFriendlyByteBuf, AssemblyBeginPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, AssemblyBeginPayload value) {
            UUIDUtil.STREAM_CODEC.encode(buffer, value.sessionId());
            MoldPattern.STREAM_CODEC.encode(buffer, value.pattern());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.material().id());
            buffer.writeVarInt(value.legalPoints().size());
            value.legalPoints().forEach(point -> {
                buffer.writeByte(point.x());
                buffer.writeByte(point.z());
            });
        }
        @Override public AssemblyBeginPayload decode(RegistryFriendlyByteBuf buffer) {
            UUID session = UUIDUtil.STREAM_CODEC.decode(buffer);
            MoldPattern pattern = MoldPattern.STREAM_CODEC.decode(buffer);
            MoldFillMaterial material = MoldFillMaterial.of(ByteBufCodecs.STRING_UTF8.decode(buffer));
            int count = buffer.readVarInt();
            if (count < 1 || count > 196) throw new IllegalArgumentException("Invalid assembly point count: " + count);
            List<ConnectionPoint> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) points.add(new ConnectionPoint(buffer.readByte(), buffer.readByte()));
            return new AssemblyBeginPayload(session, pattern, material, List.copyOf(points));
        }
    };
    AssemblyBeginPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(material, "material");
        legalPoints = List.copyOf(Objects.requireNonNull(legalPoints, "legalPoints"));
        if (legalPoints.isEmpty() || legalPoints.size() > 196) throw new IllegalArgumentException("Invalid assembly points");
        if (new HashSet<>(legalPoints).size() != legalPoints.size()) throw new IllegalArgumentException("Duplicate assembly points");
    }
    @Override public Type<AssemblyBeginPayload> type() { return TYPE; }
    record ConnectionPoint(byte x, byte z) {
        ConnectionPoint {
            if (x < 1 || x > 14 || z < 1 || z > 15) {
                throw new IllegalArgumentException("Assembly point outside mold surface: " + x + "," + z);
            }
        }
    }
}
