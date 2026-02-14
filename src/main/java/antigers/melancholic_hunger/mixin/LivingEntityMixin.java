package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.HungerEffectOption;
import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Replace hunger effect with poison effect. Decreases duration of the effect 2 times
     */
    @WrapMethod(
        method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"
    )
    boolean melancholic_hunger$addStatusEffect(
            StatusEffectInstance effect, Entity source, Operation<Boolean> original
    ) {
        if (!(((LivingEntity) (Object) this) instanceof PlayerEntity)) {
            return false;
        }
        if (effect.getEffectType() == StatusEffects.HUNGER) {
            HungerEffectOption hungerEffect = YACLConfig.hungerEffect();
            if (hungerEffect == HungerEffectOption.DISABLED) {
                return false;
            }
            else if (hungerEffect == HungerEffectOption.REPLACED_WITH_POISON) {
                effect = new StatusEffectInstance(
                        StatusEffects.POISON, effect.getDuration() / 2, effect.getAmplifier()
                );
            }
        }
        else if (InstalledMods.FARMERS_DELIGHT) {
            effect = NourishmentEffectHandler.getEffectToApply(effect);
        }
        return original.call(effect, source);
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
