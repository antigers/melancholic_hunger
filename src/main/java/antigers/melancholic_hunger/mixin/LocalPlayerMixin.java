package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    @Shadow @Final protected Minecraft minecraft;

    public LocalPlayerMixin(ClientLevel pClientLevel, GameProfile pGameProfile, @Nullable ProfilePublicKey pProfilePublicKey) {
        super(pClientLevel, pGameProfile, pProfilePublicKey);
    }

    /**
     * Allowing player to sprint only if they have more than 3 hearts (or custom amount)
     */
    @WrapOperation(
            method = "aiStep",
            at = @At(
                    value="INVOKE",
                    target="Lnet/minecraft/world/food/FoodData;getFoodLevel()I"
            )
    )
    private int melancholic_hunger$canPlayerSprint(FoodData instance, Operation<Integer> original) {
        switch (YACLConfig.sprinting()) {
            case DISABLED -> {
                return 0;
            }
            case LIMITED_BY_HEALTH -> {
                if (this.getHealth() <= YACLConfig.sprintingHealthLimit()) {
                    return 0;
                }
            }
        }
        if (YACLConfig.disableHunger()) {
            return 10;
        }
        return original.call(instance);
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
