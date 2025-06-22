package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocatorBar.class)
public class LocatorBarMixin {
    @Unique DrawHudContext drawHudContext;

    /**
     * Makes the locator bar to follow the animation
     */
    @WrapOperation(
            method="renderBar",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
            )
    )
    private void melancholic_hunger$renderBar(
            DrawContext drawContext, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        var drawHudContext = (DrawHudContext) drawContext;
        y += 7 - drawHudContext.getBarAnimation().getCurrentPos();
        original.call(drawContext, pipeline, sprite, x, y, width, height);
        drawHudContext.locatorBarWasRendered = true;
    }

    /**
     * Makes so that addons only render when the locator bar itself was actually rendered in this frame
     */
    @WrapMethod(method="renderAddons")
    private void melancholic_hunger$renderAddons(
            DrawContext drawContext, RenderTickCounter tickCounter, Operation<Void> original
    ) {
        drawHudContext = (DrawHudContext) drawContext;
        if (drawHudContext.locatorBarWasRendered) {
            original.call(drawContext, tickCounter);
        }
    }

    /**
     * Makes the addons of the locator bar to follow the animation
     */
    @ModifyExpressionValue(
            method="renderAddons",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/bar/LocatorBar;getCenterY(Lnet/minecraft/client/util/Window;)I"
            )
    )
    private int melancholic_hunger$renderAddonsModifyValueOfY(int original) {
        return original + 7 - drawHudContext.getBarAnimation().getCurrentPos();
    }
}
