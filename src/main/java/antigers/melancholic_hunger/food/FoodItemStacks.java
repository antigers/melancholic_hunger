package antigers.melancholic_hunger.food;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

import java.util.Map;
import java.util.Optional;

public class FoodItemStacks {
    private static void modifyMaxStackSizeComponent(DefaultItemComponentEvents.ModifyContext context) {
        Map<Item, Integer> maxStackSizes = MelancholicConfig.getItemStackSizes();
        if (maxStackSizes == null) {
            return;
        }
        context.modify(
                maxStackSizes::containsKey,
                (builder, item) -> {
                    builder.set(DataComponents.MAX_STACK_SIZE, maxStackSizes.get(item));
                }
        );
    }

    public static int getHealthFromMatchaRegeneration(ItemStack itemStack) {
        Consumable consumableComponent = itemStack.get(DataComponents.CONSUMABLE);
        if (consumableComponent == null) {
            return 0;
        }
        Optional<ConsumeEffect> consumeEffectOptional = consumableComponent.onConsumeEffects().stream()
                .filter(effect -> effect.getType() == ConsumeEffect.Type.APPLY_EFFECTS)
                .findFirst();
        if (consumeEffectOptional.isEmpty()) {
            return 0;
        }
        ApplyStatusEffectsConsumeEffect consumeEffect = (ApplyStatusEffectsConsumeEffect) consumeEffectOptional.get();
        Optional<MobEffectInstance> regenerationEffect = consumeEffect.effects().stream().
                filter(effect -> effect.is(MobEffects.REGENERATION) && effect.getAmplifier() == 2)
                .findFirst();
        return regenerationEffect.map(effect -> effect.getDuration() / 12).orElse(0);
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(FoodItemStacks::modifyMaxStackSizeComponent);
    }
}
