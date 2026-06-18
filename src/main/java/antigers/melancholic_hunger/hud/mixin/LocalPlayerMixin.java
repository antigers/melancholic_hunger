package antigers.melancholic_hunger.hud.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow
    public int experienceDisplayStartTick;

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Makes so that the experience bar is drawn only if the experience value has actually been changed
     */
    @WrapMethod(method = "setExperienceValues")
    private void melancholic_hunger$setExperience(float progress, int total, int level, Operation<Void> original) {
        // checking age to see if player is fully initialized
        if (this.tickCount > 0 && total > this.totalExperience) {
            this.experienceDisplayStartTick = this.tickCount;
        }
        original.call(progress, total, level);
    }

    /**
     * Makes that vanilla way of setting experienceDisplayStartTick to the player age isn't used
     */
    @ModifyExpressionValue(
            method = "setExperienceDisplayStartTickToTickCount",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;tickCount:I"
            )
    )
    private int melancholic_hunger$setExperience(int original) {
        return this.experienceDisplayStartTick;
    }
}
