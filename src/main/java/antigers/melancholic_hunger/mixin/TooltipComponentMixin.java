package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.FoodItemTooltips;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TooltipComponent.class)
public interface TooltipComponentMixin extends TooltipComponent {
	@WrapMethod(
			method = "of(Lnet/minecraft/text/OrderedText;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;"
	)
	private static TooltipComponent melancholic_hunger$of(OrderedText text, Operation<TooltipComponent> original) {
		if (text instanceof FoodItemTooltips.FoodHealthTextComponent foodHealthTextComponent) {
			return foodHealthTextComponent.getComponent();
		}
		return original.call(text);
	}
}
