package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.PlayerComponents;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodComponent.class)
public class FoodComponentMixin {

    /**
     * Restores player's health after eating food
     */
    @Inject(
            method = "onConsume",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/HungerManager;eat(Lnet/minecraft/component/type/FoodComponent;)V"
            )
    )
    private void melancholic_hunger$playerEatFood(
            World world, LivingEntity user, ItemStack itemStack, ConsumableComponent consumable, CallbackInfo callback
    ) {
        if (user instanceof PlayerEntity) {
            PlayerComponents.HEALTH_REGENERATION.get(user).eat(itemStack, (FoodComponent) (Object) this);
        }
    }
}
