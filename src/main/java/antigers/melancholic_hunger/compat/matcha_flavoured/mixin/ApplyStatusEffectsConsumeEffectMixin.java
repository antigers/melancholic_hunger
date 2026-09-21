package antigers.melancholic_hunger.compat.matcha_flavoured.mixin;

import antigers.melancholic_hunger.compat.matcha_flavoured.MatchaFoodItems;
import antigers.melancholic_hunger.compat.matcha_flavoured.MatchaInstalledFlag;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
public class ApplyStatusEffectsConsumeEffectMixin {
    /**
     * Makes so that Matcha Flavoured's regeneration effect on food items is ignored, because health regen is handled by Melancholic
     */
    @WrapOperation(
            method="apply",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
            )
    )
    private boolean melancholic_hunger$addEffectLimiter(
            LivingEntity instance, MobEffectInstance newEffect, Operation<Boolean> original, @Local(argsOnly = true) ItemStack stack
    ) {
        if (
                MatchaInstalledFlag.get(instance.level()) && stack.has(DataComponents.FOOD)
                        && MatchaFoodItems.getFoodNutritionFromEffect(newEffect) > 0
        ) {
            return false;
        }
        return original.call(instance, newEffect);
    }
}
