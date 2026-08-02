package moe.liar.trms;

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

/** Minecraft serialization adapter for one completed, player-shaped weapon part item. */
record TrmsWeaponPart(TrmsMoldPattern pattern, MoldFillMaterial material) {
    private static final Codec<MoldFillMaterial> MATERIAL_CODEC = Codec.STRING.comapFlatMap(
            TrmsWeaponPart::decodeMaterial,
            MoldFillMaterial::id
    );
    private static final StreamCodec<ByteBuf, MoldFillMaterial> MATERIAL_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(MoldFillMaterial::of, MoldFillMaterial::id);

    static final Codec<TrmsWeaponPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TrmsMoldPattern.CODEC.fieldOf("pattern").forGetter(TrmsWeaponPart::pattern),
            MATERIAL_CODEC.fieldOf("material").forGetter(TrmsWeaponPart::material)
    ).apply(instance, TrmsWeaponPart::new));
    static final StreamCodec<RegistryFriendlyByteBuf, TrmsWeaponPart> STREAM_CODEC = StreamCodec.composite(
            TrmsMoldPattern.STREAM_CODEC,
            TrmsWeaponPart::pattern,
            MATERIAL_STREAM_CODEC,
            TrmsWeaponPart::material,
            TrmsWeaponPart::new
    );

    TrmsWeaponPart {
        pattern = Objects.requireNonNull(pattern, "pattern");
        material = Objects.requireNonNull(material, "material");
        // Delegate the domain invariant to common rather than allowing an
        // endpoint-specific item adapter to produce a meaningless blank part.
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
