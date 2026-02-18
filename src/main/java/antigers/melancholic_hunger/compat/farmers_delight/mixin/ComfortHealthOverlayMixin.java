package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.ComfortEffectHandler;
import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
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
					target="Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
			)
	)
	private static void melancholic_hunger$drawComfortOverlay(
			Gui gui, PoseStack poseStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) gui).melancholic_hunger$getDrawHudContext();
		y = drawHudContext.getHelper().addShakingIfNeeded(drawHudContext.getHealthBarY());
		original.call(gui, poseStack, x, y, uOffset, vOffset, uWidth, vHeight);
	}
}
