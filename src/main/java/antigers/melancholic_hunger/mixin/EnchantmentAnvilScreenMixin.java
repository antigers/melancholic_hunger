package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin({EnchantmentScreen.class, AnvilScreen.class})
public abstract class EnchantmentAnvilScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {

    public EnchantmentAnvilScreenMixin(EnchantmentMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @ModifyExpressionValue(
            method = "containerTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;tickCount:I"
            )
    )
    private int melancholic_hunger$removeExpBarDisplay(int original) {
        if (YACLConfig.hideExperienceBar()) {
            // making so that the vanilla method of drawing exp bar on Enchantment and Anvil screens doesn't work
            // when the exp bar is hidden, because in that case we use our own "show on screens" feature
            return this.minecraft.player.experienceDisplayStartTick;
        }
        return original;
    }
}
