package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

import java.util.function.Function;

@Mixin(HUDOverlays.class)
public class HUDOverlaysMixin {
	@WrapOperation(
			method="drawComfortOverlay",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V"
			)
	)
	private static void melancholic_hunger$drawComfortOverlay(
			DrawContext graphics, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original
	) {
		DrawHudContext drawHudContext = (DrawHudContext) graphics;
		y = drawHudContext.getHelper().addShakingIfNeeded(drawHudContext.getHealthBarY());
		original.call(graphics, renderLayers, sprite, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	@WrapMethod(method="drawNourishmentOverlay")
	private static void melancholic_hunger$drawNourishmentOverlay(
			HungerManager foodData, MinecraftClient minecraft, DrawContext graphics, int right, int top, boolean naturalHealing, Operation<Void> original
	) {
		if (YACLConfig.disableHunger()) {
			return;
		}
		DrawHudContext drawHudContext = (DrawHudContext) graphics;
		original.call(foodData, minecraft, graphics, right, top + 7 - drawHudContext.getHudExperienceOffset(), naturalHealing);
	}
}
