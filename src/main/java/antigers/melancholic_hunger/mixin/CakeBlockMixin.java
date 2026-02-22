package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.food.EdibleBlockFoods;
import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {

	/**
	 * Handles eating cake, because it doesn't work the same way as all other food
	 */
	@Inject(
			method="eat",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/world/food/FoodData;eat(IF)V"
			)
	)
	private static void melancholic_hunger$eatCake(
			LevelAccessor level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir
	) {
		HealthRegenerationComponent.get(player).eat(Items.CAKE.getDefaultInstance(), EdibleBlockFoods.CAKE_PROPERTIES);
	}
}
