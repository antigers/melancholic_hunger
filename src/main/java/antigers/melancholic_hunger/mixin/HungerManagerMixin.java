package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class HungerManagerMixin {
    /**
     * Disables hunger manager from doing its logic
     */
    @Inject(
        method = "update",
        at = @At("HEAD"),
        cancellable = true
    )
    public void melancholic_hunger$disableHunger(ServerPlayerEntity player, CallbackInfo callback) {
        if (YACLConfig.disableHunger()) {
            callback.cancel();
        }
    }
}
