package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    /**
     * Disables hunger manager from doing its logic
     */
    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void melancholic_hunger$disableHunger(Player player, CallbackInfo callback) {
        if (YACLConfig.disableHunger()) {
            callback.cancel();
        }
    }
}
