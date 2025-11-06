package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.Cake;
import antigers.melancholic_hunger.components.PlayerComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
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
			method="tryEat",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/entity/player/HungerManager;add(IF)V"
			)
	)
	private static void melancholic_hunger$eatCake(
			WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> cir
	) {
		PlayerComponents.HEALTH_REGENERATION.get(player).eat(Items.CAKE.getDefaultStack(), Cake.FOOD_COMPONENT);
	}
}
