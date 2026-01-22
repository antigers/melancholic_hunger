package antigers.melancholic_hunger.hud.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocatorBarRenderer.class)
public class LocatorBarRendererMixin {
    @Unique DrawHudContext drawHudContext;

    /**
     * Makes the locator bar to follow the animation
     */
    @WrapOperation(
            method="renderBackground",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void melancholic_hunger$renderBar(
            GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        var drawHudContext = (DrawHudContext) guiGraphics;
        y += 7 - drawHudContext.getBarAnimation().getCurrentPos();
        original.call(guiGraphics, pipeline, sprite, x, y, width, height);
        drawHudContext.locatorBarWasRendered = true;
    }

    /**
     * Makes so that addons only render when the locator bar itself was actually rendered in this frame
     */
    @WrapMethod(method="render")
    private void melancholic_hunger$renderAddons(
            GuiGraphics guiGraphics, DeltaTracker tickCounter, Operation<Void> original
    ) {
        drawHudContext = (DrawHudContext) guiGraphics;
        if (drawHudContext.locatorBarWasRendered) {
            original.call(guiGraphics, tickCounter);
        }
    }

    /**
     * Makes the addons of the locator bar to follow the animation
     */
    @ModifyExpressionValue(
            method="render",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/LocatorBarRenderer;top(Lcom/mojang/blaze3d/platform/Window;)I"
            )
    )
    private int melancholic_hunger$renderAddonsModifyValueOfY(int original) {
        return original + 7 - drawHudContext.getBarAnimation().getCurrentPos();
    }
}
