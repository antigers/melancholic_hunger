package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin({EnchantmentScreen.class, AnvilScreen.class})
public abstract class EnchantmentAnvilScreenMixin extends HandledScreen<EnchantmentScreenHandler> {

    public EnchantmentAnvilScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @ModifyExpressionValue(
            method = "handledScreenTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;age:I"
            )
    )
    private int melancholic_hunger$removeExpBarDisplay(int original) {
        if (YACLConfig.hideExperienceBar()) {
            // making so that the vanilla method of drawing exp bar on Enchantment and Anvil screens doesn't work
            // when the exp bar is hidden, because in that case we use our own show on screens feature
            return this.client.player.experienceBarDisplayStartTime;
        }
        return original;
    }
}
