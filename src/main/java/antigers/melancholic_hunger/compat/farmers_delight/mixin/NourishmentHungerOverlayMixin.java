package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import vectorwing.farmersdelight.client.gui.NourishmentHungerOverlay;

@Mixin(NourishmentHungerOverlay.class)
public class NourishmentHungerOverlayMixin {
	@WrapMethod(method="drawNourishmentOverlay", remap=false)
	private static void melancholic_hunger$drawNourishmentOverlay(
			FoodData foodData, Minecraft minecraft, GuiGraphics graphics, int right, int top, boolean naturalHealing, Operation<Void> original
	) {
		if (MelancholicConfig.disableHunger()) {
			return;
		}
		original.call(foodData, minecraft, graphics, right, top, naturalHealing);
	}
}
