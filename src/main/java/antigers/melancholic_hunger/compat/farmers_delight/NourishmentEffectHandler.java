package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import vectorwing.farmersdelight.common.effect.NourishmentEffect;
import vectorwing.farmersdelight.common.registry.ModEffects;

public class NourishmentEffectHandler {
//	public static final Holder<MobEffect> NOURISHMENT_HEALTH_BOOST = Registry.registerForHolder(
//			BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "nourishment_health_boost"), new NourishmentHealthBoostEffect()
//					.addAttributeModifier(Attributes.MAX_HEALTH, Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "effect.health_boost"), 2.0, AttributeModifier.Operation.ADD_VALUE)
//	);

//	private static class NourishmentHealthBoostEffect extends NourishmentEffect {
//		public String getDescriptionId() {
//			return ModEffects.NOURISHMENT.value().getDescriptionId();
//		}
//
//		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
//			return false;
//		}
//	}

	public static MobEffectInstance getEffectToApply(MobEffectInstance effect) {
		Holder<MobEffect> effectType = effect.getEffect();
//		if (effectType != ModEffects.NOURISHMENT || !YACLConfig.disableHunger()) {
//			return effect;
//		}
		int nourishmentHealthBoostHeartsCount = YACLConfig.nourishmentHealthBoostHeartsCount();
		int amplifier;
		if (nourishmentHealthBoostHeartsCount > 0) {
//			effectType = NOURISHMENT_HEALTH_BOOST;
			amplifier = nourishmentHealthBoostHeartsCount - 1;
		}
		else {
			amplifier = effect.getAmplifier();
		}
		return new MobEffectInstance(
				effectType, effect.getDuration(), amplifier, effect.isAmbient(), false, true
		);
	}

	public static boolean isNourishmentHealthBoost(Holder<MobEffect> effect) {
		return false;
//		return effect == NOURISHMENT_HEALTH_BOOST;
	}

	public static Holder<MobEffect> getEffectForSprite(Holder<MobEffect> effect) {
		// making nourishment health boost effect use standard nourishment's sprite
		if (isNourishmentHealthBoost(effect)) {
//			return ModEffects.NOURISHMENT;
		}
		return effect;
	}

	public static boolean playerHasEffect(Player player) {
		return false;
//		return player.hasEffect(ModEffects.NOURISHMENT) || player.hasEffect(NOURISHMENT_HEALTH_BOOST);
	}

	public static void register() {}
}
