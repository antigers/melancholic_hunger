package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.HungerEffectOption;
import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source);

    /**
     * Replace hunger effect with poison effect. Decreases duration of the effect 2 times
     */
    @Inject(
        at=@At(value="HEAD"),
        method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
        cancellable=true
    )
    void melancholic_hunger$addPoisonInsteadOfHunger(
            StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback
    ) {
        if (
                ((LivingEntity) (Object) this) instanceof PlayerEntity &&
                effect.getEffectType() == StatusEffects.HUNGER
        ) {
            var hungerEffect = YACLConfig.hungerEffect();
            if (hungerEffect == HungerEffectOption.DISABLED) {
                callback.setReturnValue(false);
            }
            else if (hungerEffect == HungerEffectOption.REPLACED_WITH_POISON) {
                StatusEffectInstance poisonEffect = new StatusEffectInstance(
                        StatusEffects.POISON, effect.getDuration() / 2, effect.getAmplifier()
                );
                callback.setReturnValue(this.addStatusEffect(poisonEffect, source));
            }
        }
    }

    /**
     * Enables instant eating
     */
    @WrapOperation(
            method="setCurrentHand",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/item/ItemStack;getMaxUseTime(Lnet/minecraft/entity/LivingEntity;)I"
            )
    )
    private int melancholic_hunger$setCurrentHandMaxUseTime(
            ItemStack stack, LivingEntity user, Operation<Integer> original
    ) {
        if (YACLConfig.instantEating() && stack.get(DataComponentTypes.FOOD) != null) {
            return 1;
        }
        return original.call(stack, user);
    }
}
