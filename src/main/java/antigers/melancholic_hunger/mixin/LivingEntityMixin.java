package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.HungerEffectOption;
import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract boolean addEffect(MobEffectInstance effect, @Nullable Entity source);

    /**
     * Replace hunger effect with poison effect. Decreases duration of the effect 2 times
     */
    @Inject(
        at=@At(value="HEAD"),
        method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        cancellable=true
    )
    void melancholic_hunger$addPoisonInsteadOfHunger(
            MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback
    ) {
        if (
                ((LivingEntity) (Object) this) instanceof Player &&
                effect.getEffect() == MobEffects.HUNGER
        ) {
            var hungerEffect = YACLConfig.hungerEffect();
            if (hungerEffect == HungerEffectOption.DISABLED) {
                callback.setReturnValue(false);
            }
            else if (hungerEffect == HungerEffectOption.REPLACED_WITH_POISON) {
                MobEffectInstance poisonEffect = new MobEffectInstance(
                        MobEffects.POISON, effect.getDuration() / 2, effect.getAmplifier()
                );
                callback.setReturnValue(this.addEffect(poisonEffect, source));
            }
        }
    }

    /**
     * Enables instant eating
     */
    @WrapOperation(
            method="startUsingItem",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/world/item/ItemStack;getUseDuration()I"
            )
    )
    private int melancholic_hunger$setCurrentHandMaxUseTime(
            ItemStack stack, Operation<Integer> original
    ) {
        if (YACLConfig.shouldInstantlyEat(stack.getItem()) && stack.getItem().getFoodProperties() != null) {
            return 1;
        }
        return original.call(stack);
    }
}
