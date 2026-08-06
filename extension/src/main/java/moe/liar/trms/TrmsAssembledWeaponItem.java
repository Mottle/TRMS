package moe.liar.trms;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Demo assembled weapon item; combat behaviour is intentionally deferred. */
@SuppressWarnings("deprecation")
final class TrmsAssembledWeaponItem extends Item {
    TrmsAssembledWeaponItem(Properties properties, net.minecraft.resources.ResourceKey<Item> key) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        TrmsAssembledWeapon assembled = stack.get(TrmsContent.assembledWeaponComponent());
        if (assembled != null) {
            tooltip.accept(Component.translatable("item.trms.assembled_weapon.material", assembled.material().id()));
        }
    }
}
