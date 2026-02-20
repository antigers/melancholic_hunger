package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow public int experienceDisplayStartTick;

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Allowing player to sprint only if they have more than 3 hearts (or custom amount)
     */
    @WrapMethod(method = "hasEnoughFoodToSprint")
    private boolean melancholic_hunger$canPlayerSprint(Operation<Boolean> original) {
        if (this.isPassenger() || this.getAbilities().mayfly) {
            return true;
        }
        switch (YACLConfig.sprinting()) {
            case DISABLED -> {
                return false;
            }
            case LIMITED_BY_HEALTH -> {
                if (this.getHealth() <= YACLConfig.sprintingHealthLimit()) {
                    return false;
                }
            }
        }
        if (YACLConfig.disableHunger()) {
            return true;
        }
        return (float)this.getFoodData().getFoodLevel() > 6.0F;
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
            method="setExperienceValues",
            at=@At(
                    value="FIELD",
                    target="Lnet/minecraft/client/player/LocalPlayer;tickCount:I"
            )
    )
    private int melancholic_hunger$setExperience(int original) {
        return this.experienceDisplayStartTick;
    }
}
