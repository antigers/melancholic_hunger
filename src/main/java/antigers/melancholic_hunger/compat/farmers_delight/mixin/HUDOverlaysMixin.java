package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

@Mixin(HUDOverlays.class)
public class HUDOverlaysMixin {
    @WrapOperation(
            method = "drawComfortOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            )
    )
    private static void melancholic_hunger$drawComfortOverlay(
            GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        y = drawHudContext.getHelper().addShakingIfNeeded(drawHudContext.getHealthBarY());
        original.call(graphics, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
    }

    @WrapMethod(method = "drawNourishmentOverlay")
    private static void melancholic_hunger$drawNourishmentOverlay(
            FoodData foodData, Minecraft minecraft, GuiGraphics graphics, int right, int top, boolean naturalHealing, Operation<Void> original
    ) {
        if (MelancholicConfig.disableHunger()) {
            return;
        }
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        original.call(foodData, minecraft, graphics, right, top + 7 - drawHudContext.getHudExperienceOffset(), naturalHealing);
    }
}
