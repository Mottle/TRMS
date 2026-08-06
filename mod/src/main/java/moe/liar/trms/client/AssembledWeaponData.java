package moe.liar.trms.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import moe.liar.trms.common.MoldAssembledWeapon;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldWeaponAssembly;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Client mirror of the completed weapon data component. */
public record AssembledWeaponData(
        MoldPattern pattern,
        MoldFillMaterial material,
        String handleMaterial,
        int connectionX,
        int connectionZ
) {
    private static final Codec<MoldFillMaterial> MATERIAL_CODEC = Codec.STRING.comapFlatMap(
            AssembledWeaponData::decodeMaterial, MoldFillMaterial::id);
    private static final StreamCodec<ByteBuf, MoldFillMaterial> MATERIAL_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(MoldFillMaterial::of, MoldFillMaterial::id);
    public static final Codec<AssembledWeaponData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MoldPattern.CODEC.fieldOf("pattern").forGetter(AssembledWeaponData::pattern),
            MATERIAL_CODEC.fieldOf("material").forGetter(AssembledWeaponData::material),
            Codec.STRING.fieldOf("handle_material").forGetter(AssembledWeaponData::handleMaterial),
            Codec.INT.fieldOf("connection_x").forGetter(AssembledWeaponData::connectionX),
            Codec.INT.fieldOf("connection_z").forGetter(AssembledWeaponData::connectionZ)
    ).apply(instance, AssembledWeaponData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssembledWeaponData> STREAM_CODEC = StreamCodec.composite(
            MoldPattern.STREAM_CODEC, AssembledWeaponData::pattern,
            MATERIAL_STREAM_CODEC, AssembledWeaponData::material,
            ByteBufCodecs.STRING_UTF8, AssembledWeaponData::handleMaterial,
            ByteBufCodecs.VAR_INT, AssembledWeaponData::connectionX,
            ByteBufCodecs.VAR_INT, AssembledWeaponData::connectionZ,
            AssembledWeaponData::new);

    public AssembledWeaponData {
        pattern = Objects.requireNonNull(pattern, "pattern");
        material = Objects.requireNonNull(material, "material");
        handleMaterial = Objects.requireNonNull(handleMaterial, "handleMaterial");
        new MoldAssembledWeapon(pattern.commonPattern(), material, handleMaterial,
                new MoldWeaponAssembly.ConnectionPoint(connectionX, connectionZ));
    }

    private static DataResult<MoldFillMaterial> decodeMaterial(String id) {
        try {
            return DataResult.success(MoldFillMaterial.of(id));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
