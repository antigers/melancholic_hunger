package antigers.melancholic_hunger.compat.matcha_flavoured;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class MatchaFoodItems {
    public static int getFoodNutritionFromEffect(MobEffectInstance effect) {
        if (!effect.is(MobEffects.REGENERATION)) {
            return 0;
        }
        switch (effect.getAmplifier()) {
            case 2 -> {
                if (effect.getDuration() % 12 == 0) {
                    return effect.getDuration() / 12;
                } else if (effect.getDuration() == 20) {
                    return 2;
                }
            }
            case 3 -> {
                if (effect.getDuration() == 15) {
                    return 3;
                }
            }
        }
        return 0;
    }

    public static FoodProperties getFoodValuesFromComponents(ItemStack itemStack, Level level) {
        if (!MatchaInstalledFlag.get(level)) {
            // Matcha Flavoured is not installed
            return null;
        }
        Consumable consumableComponent = itemStack.get(DataComponents.CONSUMABLE);
        if (consumableComponent == null) {
            return null;
        }
        Optional<ConsumeEffect> consumeEffectOptional = consumableComponent.onConsumeEffects().stream()
                .filter(effect -> effect.getType() == ConsumeEffect.Type.APPLY_EFFECTS)
                .findFirst();
        if (consumeEffectOptional.isEmpty()) {
            return null;
        }
        ApplyStatusEffectsConsumeEffect consumeEffect = (ApplyStatusEffectsConsumeEffect) consumeEffectOptional.get();
        for (MobEffectInstance effect : consumeEffect.effects()) {
            int nutrition = getFoodNutritionFromEffect(effect);
            if (nutrition <= 0) {
                continue;
            }
            return new FoodProperties(
                    nutrition, effect.getAmplifier() > 2 ? nutrition * 1.5F : nutrition, consumeEffect.effects().size() > 1
            );
        }
        return null;
    }
}
