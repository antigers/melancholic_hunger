package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import vectorwing.farmersdelight.common.effect.NourishmentEffect;
import vectorwing.farmersdelight.common.registry.ModEffects;

public class NourishmentEffectHandler {
	public static final RegistryEntry<StatusEffect> NOURISHMENT_HEALTH_BOOST = Registry.registerReference(
			Registries.STATUS_EFFECT, Identifier.of(MelancholicHunger.MOD_ID, "nourishment_health_boost"), new NourishmentHealthBoostEffect()
					.addAttributeModifier(EntityAttributes.GENERIC_MAX_HEALTH, Identifier.of(MelancholicHunger.MOD_ID, "effect.health_boost"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE)
	);

	private static class NourishmentHealthBoostEffect extends NourishmentEffect {
		public String getTranslationKey() {
			return ModEffects.NOURISHMENT.value().getTranslationKey();
		}

		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			return false;
		}
	}

	public static StatusEffectInstance getEffectToApply(StatusEffectInstance effect) {
		RegistryEntry<StatusEffect> effectType = effect.getEffectType();
		if (effectType != ModEffects.NOURISHMENT || !YACLConfig.disableHunger()) {
			return effect;
		}
		int nourishmentHealthBoostHeartsCount = YACLConfig.nourishmentHealthBoostHeartsCount();
		int amplifier;
		if (nourishmentHealthBoostHeartsCount > 0) {
			effectType = NOURISHMENT_HEALTH_BOOST;
			amplifier = nourishmentHealthBoostHeartsCount - 1;
		}
		else {
			amplifier = effect.getAmplifier();
		}
		return new StatusEffectInstance(
				effectType, effect.getDuration(), amplifier, effect.isAmbient(), false, true
		);
	}

	public static boolean isNourishmentHealthBoost(RegistryEntry<StatusEffect> effect) {
		return effect == NOURISHMENT_HEALTH_BOOST;
	}

	public static RegistryEntry<StatusEffect> getEffectForSprite(RegistryEntry<StatusEffect> effect) {
		// making nourishment health boost effect use standard nourishment's sprite
		if (isNourishmentHealthBoost(effect)) {
			return ModEffects.NOURISHMENT;
		}
		return effect;
	}

	public static boolean playerHasEffect(PlayerEntity player) {
		return player.hasStatusEffect(ModEffects.NOURISHMENT) || player.hasStatusEffect(NOURISHMENT_HEALTH_BOOST);
	}

	public static void register() {}
}
