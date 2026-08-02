package moe.liar.trms.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import moe.liar.trms.common.MoldFillMaterial;
import moe.liar.trms.common.MoldWeaponPart;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Client mirror of the weapon-part item component emitted by the Extension. */
public record WeaponPartData(MoldPattern pattern, MoldFillMaterial material) {
    private static final Codec<MoldFillMaterial> MATERIAL_CODEC = Codec.STRING.comapFlatMap(
            WeaponPartData::decodeMaterial,
            MoldFillMaterial::id
    );
    private static final StreamCodec<ByteBuf, MoldFillMaterial> MATERIAL_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(MoldFillMaterial::of, MoldFillMaterial::id);

    public static final Codec<WeaponPartData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MoldPattern.CODEC.fieldOf("pattern").forGetter(WeaponPartData::pattern),
            MATERIAL_CODEC.fieldOf("material").forGetter(WeaponPartData::material)
    ).apply(instance, WeaponPartData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponPartData> STREAM_CODEC = StreamCodec.composite(
            MoldPattern.STREAM_CODEC,
            WeaponPartData::pattern,
            MATERIAL_STREAM_CODEC,
            WeaponPartData::material,
            WeaponPartData::new
    );

    public WeaponPartData {
        pattern = Objects.requireNonNull(pattern, "pattern");
        material = Objects.requireNonNull(material, "material");
        new MoldWeaponPart(pattern.commonPattern(), material);
    }

    private static DataResult<MoldFillMaterial> decodeMaterial(String id) {
        try {
            return DataResult.success(MoldFillMaterial.of(id));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
