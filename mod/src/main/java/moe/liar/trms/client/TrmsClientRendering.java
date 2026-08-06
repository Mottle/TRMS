package moe.liar.trms.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

/** Client event handlers kept separate from the common registration declarations. */
public final class TrmsClientRendering {
    private TrmsClientRendering() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TrmsClientMod.MOLD_BLOCK_ENTITY.get(), MoldBlockEntityRenderer::new);
    }

    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_special"),
                MoldSpecialModelRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_first_person_special"),
                MoldSpecialModelRenderer.FirstPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_ground_special"),
                MoldSpecialModelRenderer.GroundUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_blank_special"),
                MoldSpecialModelRenderer.BlankUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_blank_first_person_special"),
                MoldSpecialModelRenderer.BlankFirstPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "mold_blank_ground_special"),
                MoldSpecialModelRenderer.BlankGroundUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_special"),
                WeaponPartSpecialModelRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_gui_special"),
                WeaponPartSpecialModelRenderer.GuiUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_first_person_special"),
                WeaponPartSpecialModelRenderer.FirstPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_third_person_special"),
                WeaponPartSpecialModelRenderer.ThirdPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_fixed_special"),
                WeaponPartSpecialModelRenderer.FixedUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "weapon_part_ground_special"),
                WeaponPartSpecialModelRenderer.GroundUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_special"),
                AssembledWeaponSpecialModelRenderer.Unbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_gui_special"),
                AssembledWeaponSpecialModelRenderer.GuiUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_first_person_special"),
                AssembledWeaponSpecialModelRenderer.FirstPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_third_person_special"),
                AssembledWeaponSpecialModelRenderer.ThirdPersonUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_fixed_special"),
                AssembledWeaponSpecialModelRenderer.FixedUnbaked.CODEC);
        event.register(Identifier.fromNamespaceAndPath(TrmsClientMod.MOD_ID, "assembled_weapon_ground_special"),
                AssembledWeaponSpecialModelRenderer.GroundUnbaked.CODEC);
    }
}
