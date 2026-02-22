package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.PieBlock;

@Mixin(PieBlock.class)
public class PieBlockMixin {
	@Inject(
			method="consumeBite",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
			)
	)
	private void melancholic_hunger$eatPieBite(
			Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir,
			@Local(name = "sliceFood") FoodProperties sliceFood, @Local(name = "sliceStack") ItemStack sliceStack
	) {
		HealthRegenerationComponent.get(player).eat(sliceStack, sliceFood);
	}
}
