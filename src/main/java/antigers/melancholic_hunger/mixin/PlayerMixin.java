package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import antigers.melancholic_hunger.components.PlayerComponents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Final
    @Shadow
    private Abilities abilities;

    private PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * Restores player's health after eating food
     */
    @WrapOperation(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
            )
    )
    private void melancholic_hunger$playerEatFood(
            FoodData foodData, FoodProperties foodProperties, Operation<Void> original, @Local(argsOnly = true) ItemStack itemStack
    ) {
        boolean didConsume = HealthRegenerationComponent.get((Player) (Object) this).eat(itemStack, foodProperties);
        if (!didConsume) {
            original.call(foodData, foodProperties);
        }
    }

    /**
     * Doesn't allow for the player to eat food if health is full
     */
    @WrapMethod(method = "canEat")
    private boolean melancholic_hunger$canPlayerEatFood(boolean ignoreHunger, Operation<Boolean> original) {
        if (this.abilities.invulnerable || ignoreHunger) {
            return true;
        }
        return PlayerComponents.HEALTH_REGENERATION.get(this).canEat();
    }
}
