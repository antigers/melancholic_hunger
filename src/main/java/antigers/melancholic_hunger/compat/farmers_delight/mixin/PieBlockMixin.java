package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.common.block.PieBlock;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(PieBlock.class)
public class PieBlockMixin {
	@WrapOperation(
			method="consumeBite",
			at=@At(
					value="INVOKE",
					target="Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"
			)
	)
	private void melancholic_hunger$eatPieBite(
			Stream instance, Consumer<?> consumer, Operation<Void> original, @Local(argsOnly = true) Player player,
			@Local(name = "sliceStack") ItemStack sliceStack
	) {
		boolean didConsume = HealthRegenerationComponent.get(player).eat(sliceStack, sliceStack.get(DataComponents.FOOD));
		if (!didConsume) {
			original.call(instance, consumer);
		}
	}
}
