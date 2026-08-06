package moe.liar.trms;

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

/** Minecraft data-component adapter for a completed, non-stackable demo weapon. */
record TrmsAssembledWeapon(
        TrmsMoldPattern pattern,
        MoldFillMaterial material,
        String handleMaterial,
        int connectionX,
        int connectionZ
) {
    private static final Codec<MoldFillMaterial> MATERIAL_CODEC = Codec.STRING.comapFlatMap(
            TrmsAssembledWeapon::decodeMaterial,
            MoldFillMaterial::id);
    private static final StreamCodec<ByteBuf, MoldFillMaterial> MATERIAL_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(MoldFillMaterial::of, MoldFillMaterial::id);

    static final Codec<TrmsAssembledWeapon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TrmsMoldPattern.CODEC.fieldOf("pattern").forGetter(TrmsAssembledWeapon::pattern),
            MATERIAL_CODEC.fieldOf("material").forGetter(TrmsAssembledWeapon::material),
            Codec.STRING.fieldOf("handle_material").forGetter(TrmsAssembledWeapon::handleMaterial),
            Codec.INT.fieldOf("connection_x").forGetter(TrmsAssembledWeapon::connectionX),
            Codec.INT.fieldOf("connection_z").forGetter(TrmsAssembledWeapon::connectionZ)
    ).apply(instance, TrmsAssembledWeapon::new));

    static final StreamCodec<RegistryFriendlyByteBuf, TrmsAssembledWeapon> STREAM_CODEC = StreamCodec.composite(
            TrmsMoldPattern.STREAM_CODEC, TrmsAssembledWeapon::pattern,
            MATERIAL_STREAM_CODEC, TrmsAssembledWeapon::material,
            ByteBufCodecs.STRING_UTF8, TrmsAssembledWeapon::handleMaterial,
            ByteBufCodecs.VAR_INT, TrmsAssembledWeapon::connectionX,
            ByteBufCodecs.VAR_INT, TrmsAssembledWeapon::connectionZ,
            TrmsAssembledWeapon::new);

    TrmsAssembledWeapon {
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
