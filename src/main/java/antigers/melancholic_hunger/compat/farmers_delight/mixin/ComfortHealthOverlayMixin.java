package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.ComfortEffectHandler;
import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.client.gui.ComfortHealthOverlay;

@Mixin(ComfortHealthOverlay.class)
public class ComfortHealthOverlayMixin {
	@WrapOperation(
			method="renderComfortOverlay",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/world/food/FoodData;getSaturationLevel()F"
			)
	)
	private static float melancholic_hunger$comfortEffectHUDSaturationCheck(
			FoodData instance, Operation<Float> original, @Local(name = "player") Player player
	) {
		return ComfortEffectHandler.shouldApply(player) ? 0.0F : 1.0F;
	}

	@WrapOperation(
			method="drawComfortOverlay",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
			)
	)
	private static void melancholic_hunger$drawComfortOverlay(
			GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		DrawHudContext drawHudContext = (DrawHudContext) graphics;
		y = drawHudContext.getHelper().addShakingIfNeeded(drawHudContext.getHealthBarY());
		original.call(graphics, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
	}
}
