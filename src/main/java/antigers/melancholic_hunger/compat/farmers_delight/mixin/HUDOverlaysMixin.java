package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

@Mixin(HUDOverlays.class)
public class HUDOverlaysMixin {
	@WrapOperation(
			method="drawComfortOverlay",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
			)
	)
	private static void melancholic_hunger$drawComfortOverlay(
			GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original
	) {
		DrawHudContext drawHudContext = (DrawHudContext) graphics;
		y = drawHudContext.getHelper().addShakingIfNeeded(drawHudContext.getHealthBarY());
		original.call(graphics, pipeline, atlas, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	@WrapMethod(method="drawNourishmentOverlay")
	private static void melancholic_hunger$drawNourishmentOverlay(
			FoodData foodData, Minecraft minecraft, GuiGraphicsExtractor graphics, int right, int top, boolean naturalHealing, Operation<Void> original
	) {
		if (YACLConfig.disableHunger()) {
			return;
		}
		original.call(foodData, minecraft, graphics, right, top, naturalHealing);
	}
}
