package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.adrenix.nostalgic.helper.candy.hud.HudHelper;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HudHelper.class)
public class HudHelperMixin {
    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lmod/adrenix/nostalgic/helper/gameplay/stamina/StaminaRenderer;isVisible()Z"
            ),
            remap = false
    )
    private static boolean melancholic_hunger$staminaIsNotVisible(Operation<Boolean> original) {
        return false;
    }

    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lmod/adrenix/nostalgic/helper/gameplay/stamina/StaminaRenderer;render(Lnet/minecraft/client/gui/DrawContext;I)V"
            ),
            remap = true
    )
    private static void melancholic_hunger$disableStaminaRendering(DrawContext sprite, int x, Operation<Void> original) {
    }
}
