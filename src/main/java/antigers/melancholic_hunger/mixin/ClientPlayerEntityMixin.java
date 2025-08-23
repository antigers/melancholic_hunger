package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    @Shadow @Final protected Minecraft minecraft;

    public ClientPlayerEntityMixin(ClientLevel clientLevel, GameProfile profile) {
        super(clientLevel, profile);
    }

    /**
     * Allowing player to sprint only if they have more than 3 hearts (or custom amount)
     */
    @WrapMethod(method = "hasEnoughFoodToStartSprinting")
    private boolean melancholic_hunger$canPlayerSprint(Operation<Boolean> original) {
        if (this.isPassenger() || this.mayFly()) {
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
     * Draw experience bar on experience gain
     */
    @Inject(
            method="setExperienceValues",
            at=@At("HEAD")
    )
    private void melancholic_hunger$drawExpBarOnExpGain(float progress, int total, int level, CallbackInfo callback) {
        // checking age to see if player is fully initialized
        if (this.tickCount > 0 && total > this.totalExperience) {
            ExperienceHudRenderer inGameHud = (ExperienceHudRenderer) this.minecraft.gui;
            inGameHud.melancholic_hunger$onAddExperience();
        }
    }
}
