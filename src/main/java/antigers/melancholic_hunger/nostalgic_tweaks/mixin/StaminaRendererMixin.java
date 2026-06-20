package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mod.adrenix.nostalgic.helper.gameplay.stamina.StaminaRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StaminaRenderer.class)
public class StaminaRendererMixin {
    @WrapMethod(method = "render", remap = false)
    private static void melancholic_hunger$renderStamina(GuiGraphics graphics, int rightHeight, Operation<Void> original) {
        if (graphics instanceof DrawHudContext drawHudContext && drawHudContext.getIsStaminaRenderingActuallyHappening()) {
            original.call(graphics, rightHeight);
        }
    }
}
