package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.common.block.PieBlock;

@Mixin(PieBlock.class)
public class PieBlockMixin {
	@WrapOperation(
			method="consumeBite",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
			)
	)
	private void melancholic_hunger$eatPieBite(
			FoodData foodData, FoodProperties foodProperties, Operation<Void> original, @Local(argsOnly = true) Player player,
			@Local(name = "sliceFood") FoodProperties sliceFood, @Local(name = "sliceStack") ItemStack sliceStack
	) {
		boolean didConsume = HealthRegenerationComponent.get(player).eat(sliceStack, sliceFood);
		if (!didConsume) {
			original.call(foodData, foodProperties);
		}
	}
}
