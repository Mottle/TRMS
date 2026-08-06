package moe.liar.trms.client;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Client registry mirror for the assembled weapon's material tooltip. */
@SuppressWarnings("deprecation")
final class AssembledWeaponItem extends Item {
    AssembledWeaponItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        AssembledWeaponData assembled = stack.get(TrmsClientMod.ASSEMBLED_WEAPON.get());
        if (assembled != null) {
            tooltip.accept(Component.translatable("item.trms.assembled_weapon.material", assembled.material().id()));
        }
    }
}
