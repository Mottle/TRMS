package moe.liar.trms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import moe.liar.trms.common.MoldFillMaterial;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrmsWeaponPartTest {
    @BeforeAll
    static void initializeMinecraftForRegistryFriendlyBuffers() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void componentAdapterRoundTripsTheCompletedPatternAndMaterialInBothFormats() {
        TrmsWeaponPart part = new TrmsWeaponPart(
                TrmsMoldPattern.empty().carve(6, 6).carve(7, 6), MoldFillMaterial.IRON);

        TrmsWeaponPart persistent = TrmsWeaponPart.CODEC.parse(JsonOps.INSTANCE,
                TrmsWeaponPart.CODEC.encodeStart(JsonOps.INSTANCE, part)
                        .getOrThrow(error -> new AssertionError(error)))
                .getOrThrow(error -> new AssertionError(error));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        TrmsWeaponPart.STREAM_CODEC.encode(buffer, part);

        assertEquals(part, persistent);
        assertEquals(part, TrmsWeaponPart.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void componentAdapterRejectsAWeaponPartWithoutAnyCastingCells() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrmsWeaponPart(TrmsMoldPattern.empty(), MoldFillMaterial.COPPER));
    }
}
