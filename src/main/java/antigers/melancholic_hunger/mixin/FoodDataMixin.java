package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Unique
    private Player melancholic_hunger$player = null;

    /**
     * Disables hunger manager from doing its logic
     */
    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void melancholic_hunger$disableHunger(Player player, CallbackInfo callback) {
        if (melancholic_hunger$player == null) {
            melancholic_hunger$player = player;
        }
        if (MelancholicConfig.disableHunger()) {
            callback.cancel();
        }
    }

    @WrapMethod(method = "eat(IF)V")
    private void melancholic_hunger$consumeFoodProperties(int foodLevel, float saturationLevel, Operation<Void> original) {
        if (melancholic_hunger$player instanceof ServerPlayer && MelancholicConfig.disableHunger()) {
            HealthRegenerationComponent.get(melancholic_hunger$player).eat(foodLevel, saturationLevel, 0);
            return;
        }
        original.call(foodLevel, saturationLevel);
    }

    /**
     * Restores player's health after eating food
     */
    @WrapOperation(
            method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"
            )
    )
    private void melancholic_hunger$playerEatFood(
            FoodData foodData, int foodLevelModifier, float saturationLevelModifier, Operation<Void> original,
            @Local(argsOnly = true) ItemStack itemStack, @Local(argsOnly = true) Item item
    ) {
        boolean didConsume = false;
        if (melancholic_hunger$player != null) {
            didConsume = HealthRegenerationComponent.get(melancholic_hunger$player).eat(itemStack, item.getFoodProperties());
        }
        if (!didConsume) {
            original.call(foodData, foodLevelModifier, saturationLevelModifier);
        }
    }
}
