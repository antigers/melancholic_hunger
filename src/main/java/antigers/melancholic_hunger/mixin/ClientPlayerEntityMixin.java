package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Shadow @Final protected MinecraftClient client;
    @Shadow public int experienceBarDisplayStartTime;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Allowing player to sprint only if they have more than 3 hearts (or custom amount)
     */
    @WrapMethod(method = "canSprint")
    private boolean melancholic_hunger$canPlayerSprint(Operation<Boolean> original) {
        if (this.hasVehicle() || this.getAbilities().allowFlying) {
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
        return (float)this.getHungerManager().getFoodLevel() > 6.0F;
    }

    /**
     * Makes so that the experience bar is drawn only if the experience value has actually been changed
     */
    @WrapMethod(method = "setExperience")
    private void melancholic_hunger$setExperience(float progress, int total, int level, Operation<Void> original) {
        // checking age to see if player is fully initialized
        if (this.age > 0 && total > this.totalExperience) {
            this.experienceBarDisplayStartTime = this.age;
        }
        original.call(progress, total, level);
    }

    /**
     * Makes that vanilla way of setting experienceBarDisplayStartTime to the player age isn't used
     */
    @ModifyExpressionValue(
            method="setExperience",
            at=@At(
                    value="FIELD",
                    target="Lnet/minecraft/client/network/ClientPlayerEntity;age:I"
            )
    )
    private int melancholic_hunger$setExperience(int original) {
        return this.experienceBarDisplayStartTime;
    }
}
