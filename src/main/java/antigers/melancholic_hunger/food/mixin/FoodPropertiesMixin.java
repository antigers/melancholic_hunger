package antigers.melancholic_hunger.food.mixin;

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

@Mixin(FoodProperties.class)
public class FoodPropertiesMixin {

    /**
     * Restores player's health after eating food
     */
    @WrapOperation(
            method = "onConsume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
            )
    )
    private void melancholic_hunger$playerEatFood(
            FoodData foodData, FoodProperties foodProperties, Operation<Void> original, @Local(name = "player") Player player,
            @Local(argsOnly = true) ItemStack itemStack
    ) {
        boolean didConsume = HealthRegenerationComponent.get(player).eat(itemStack, foodProperties);
        if (!didConsume) {
            original.call(foodData, foodProperties);
        }
    }
}
