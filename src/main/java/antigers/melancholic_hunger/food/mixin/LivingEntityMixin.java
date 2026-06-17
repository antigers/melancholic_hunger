package antigers.melancholic_hunger.food.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.HungerEffectOption;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
            return original.call(effect, source);
        }
        if (effect.getEffect() == MobEffects.HUNGER && MelancholicConfig.disableHunger()) {
            HungerEffectOption hungerEffect = MelancholicConfig.hungerEffect();
            if (hungerEffect == HungerEffectOption.DISABLED) {
                return false;
            }
            else if (hungerEffect == HungerEffectOption.REPLACED_WITH_OTHER) {
                effect = new MobEffectInstance(
                        MelancholicConfig.hungerReplacementEffect(),
                        (int) (effect.getDuration() * MelancholicConfig.hungerReplacementDurationMultiplier()),
                        effect.getAmplifier()
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
                    target="Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"
            )
    )
    private int melancholic_hunger$setCurrentHandMaxUseTime(
            ItemStack stack, LivingEntity user, Operation<Integer> original
    ) {
        if (MelancholicConfig.instantEating() && stack.get(DataComponents.FOOD) != null) {
            return 1;
        }
        return original.call(stack, user);
    }
}
