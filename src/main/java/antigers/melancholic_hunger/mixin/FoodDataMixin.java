package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
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
        if (YACLConfig.disableHunger()) {
            callback.cancel();
        }
    }

    @WrapMethod(method = "add")
    private void melancholic_hunger$consumeFoodProperties(int foodLevel, float saturationLevel, Operation<Void> original) {
        if (melancholic_hunger$player instanceof ServerPlayer && YACLConfig.disableHunger()) {
            HealthRegenerationComponent.get(melancholic_hunger$player).eat(foodLevel, saturationLevel, 0);
            return;
        }
        original.call(foodLevel, saturationLevel);
    }
}
