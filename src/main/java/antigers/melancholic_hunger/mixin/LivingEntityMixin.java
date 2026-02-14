package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.HungerEffectOption;
import antigers.melancholic_hunger.config.YACLConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Replace hunger effect with poison effect. Decreases duration of the effect 2 times
     */
    @WrapMethod(
        method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"
    )
    boolean melancholic_hunger$addStatusEffect(
            MobEffectInstance effect, Entity source, Operation<Boolean> original
    ) {
        if (!(((LivingEntity) (Object) this) instanceof Player)) {
            return false;
        }
        if (effect.getEffect() == MobEffects.HUNGER) {
            HungerEffectOption hungerEffect = YACLConfig.hungerEffect();
            if (hungerEffect == HungerEffectOption.DISABLED) {
                return false;
            }
            else if (hungerEffect == HungerEffectOption.REPLACED_WITH_POISON) {
                effect = new MobEffectInstance(
                        MobEffects.POISON, effect.getDuration() / 2, effect.getAmplifier()
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
