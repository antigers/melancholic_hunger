package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import vectorwing.farmersdelight.client.gui.NourishmentHungerOverlay;

@Mixin(NourishmentHungerOverlay.class)
public class NourishmentHungerOverlayMixin {
	@WrapMethod(method="drawNourishmentOverlay", remap=false)
	private static void melancholic_hunger$drawNourishmentOverlay(
			FoodData foodData, Minecraft minecraft, PoseStack poseStack, int right, int top, boolean naturalHealing, Operation<Void> original
	) {
		if (YACLConfig.disableHunger()) {
			return;
		}
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) minecraft.gui).melancholic_hunger$getDrawHudContext();
		original.call(foodData, minecraft, poseStack, right, top + 7 - drawHudContext.getHudExperienceOffset(), naturalHealing);
	}
}
