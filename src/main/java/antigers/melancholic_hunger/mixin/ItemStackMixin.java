package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @WrapMethod(method = "getMaxStackSize")
    private int melancholic_hunger$customStackSize(Operation<Integer> original) {
        var stackSize = YACLConfig.getItemStackSize((ItemStack)(Object) this);
        return stackSize != null ? stackSize : original.call();
    }
}
