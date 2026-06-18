package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.FoodItemTooltips;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientTooltipComponent.class)
public interface TooltipComponentMixin {
    @WrapMethod(
            method = "create(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;"
    )
    private static ClientTooltipComponent melancholic_hunger$of(FormattedCharSequence text, Operation<ClientTooltipComponent> original) {
        if (text instanceof FoodItemTooltips.FoodHealthTextComponent foodHealthTextComponent) {
            return foodHealthTextComponent.getComponent();
        }
        return original.call(text);
    }
}
