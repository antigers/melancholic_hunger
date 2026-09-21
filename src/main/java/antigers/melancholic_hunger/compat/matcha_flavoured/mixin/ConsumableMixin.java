package antigers.melancholic_hunger.compat.matcha_flavoured.mixin;

import antigers.melancholic_hunger.compat.matcha_flavoured.MatchaFoodItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Consumable.class)
public class ConsumableMixin {
    @WrapOperation(
            method="canConsume",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"
            )
    )
    private boolean melancholic_hunger$canEatConsumable(
            Player player, boolean canAlwaysEat, Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack
    ) {
        if (canAlwaysEat) {
            FoodProperties foodProperties = MatchaFoodItems.getFoodValuesFromComponents(stack, player.level());
            if (foodProperties != null && !foodProperties.canAlwaysEat()) {
                canAlwaysEat = false;
            }
        }
        return original.call(player, canAlwaysEat);
    }
}
