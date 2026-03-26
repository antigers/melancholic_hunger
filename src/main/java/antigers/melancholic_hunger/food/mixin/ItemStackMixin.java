package antigers.melancholic_hunger.food.mixin;

import antigers.melancholic_hunger.config.MelancholicConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapMethod(method = "getMaxStackSize")
    private int melancholic_hunger$customStackSize(Operation<Integer> original) {
        if ((Object) this instanceof ItemStack itemStack) {
            var stackSize = MelancholicConfig.getItemStackSize(itemStack);
            return stackSize != null ? stackSize : original.call();
        }
        return original.call();
    }
}
