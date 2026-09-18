package antigers.melancholic_hunger.compat.matcha_flavoured;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class MatchaHealthRegeneration {
    public static MelancholicConfig.FoodValues ZEROES_PAIR = new MelancholicConfig.FoodValues(0, 0);

    public static MelancholicConfig.FoodValues getFoodValuesFromComponents(ItemStack itemStack, Level level) {
        if (!MatchaInstalledFlag.get(level)) {
            // Matcha Flavoured is not installed
            return ZEROES_PAIR;
        }
        Consumable consumableComponent = itemStack.get(DataComponents.CONSUMABLE);
        if (consumableComponent == null) {
            return ZEROES_PAIR;
        }
        Optional<ConsumeEffect> consumeEffectOptional = consumableComponent.onConsumeEffects().stream()
                .filter(effect -> effect.getType() == ConsumeEffect.Type.APPLY_EFFECTS)
                .findFirst();
        if (consumeEffectOptional.isEmpty()) {
            return ZEROES_PAIR;
        }
        ApplyStatusEffectsConsumeEffect consumeEffect = (ApplyStatusEffectsConsumeEffect) consumeEffectOptional.get();
        Optional<MobEffectInstance> regenerationEffect = consumeEffect.effects().stream().
                filter(effect -> effect.is(MobEffects.REGENERATION) && effect.getAmplifier() == 2)
                .findFirst();
        int nutrition = regenerationEffect.map(effect -> effect.getDuration() / 12).orElse(0);
        return new MelancholicConfig.FoodValues(nutrition, nutrition);
    }
}
