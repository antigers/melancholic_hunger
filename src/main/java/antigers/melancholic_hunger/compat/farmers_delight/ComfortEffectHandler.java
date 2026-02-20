package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.world.entity.player.Player;

public class ComfortEffectHandler {
	public static boolean shouldApply(Player player) {
		if (!YACLConfig.disableHunger()) {
			// when hunger is disabled we use standard Farmer's Delight zero saturation check
			return player.getFoodData().getSaturationLevel() <= 0.0F;
		}
		// we don't need to apply the effect if the player is already regenerating from consumed food
		return HealthRegenerationComponent.get(player).getConsumedNutrition() <= 0;
	}
}
