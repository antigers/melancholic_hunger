package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
public class FoodPropertiesMixin {

    /**
     * Restores player's health after eating food
     */
    @Inject(
            method = "onConsume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
            )
    )
    private void melancholic_hunger$playerEatFood(
            Level world, LivingEntity user, ItemStack itemStack, Consumable consumable, CallbackInfo callback
    ) {
        if (user instanceof Player player) {
            HealthRegenerationComponent.get(player).eat(itemStack, (FoodProperties) (Object) this);
        }
    }
}
