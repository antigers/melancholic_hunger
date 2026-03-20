package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vectorwing.farmersdelight.common.effect.NourishmentEffect;
import vectorwing.farmersdelight.common.registry.ModEffects;

public class NourishmentEffectHandler {
	private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MelancholicHunger.MOD_ID);

	private static final RegistryObject<MobEffect> NOURISHMENT_HEALTH_BOOST = MOB_EFFECTS.register(
			"nourishment_health_boost", () -> new NourishmentHealthBoostEffect()
					.addAttributeModifier(Attributes.MAX_HEALTH, "AD138BB6-ADC3-4489-9708-0DCA7B9453D6", 2.0, AttributeModifier.Operation.ADDITION)
	);

	private static class NourishmentHealthBoostEffect extends NourishmentEffect {
		public String getDescriptionId() {
			return ModEffects.NOURISHMENT.get().getDescriptionId();
		}

		public boolean isDurationEffectTick(int duration, int amplifier) {
			return false;
		}
	}

	public static MobEffectInstance getEffectToApply(MobEffectInstance effect) {
		MobEffect effectType = effect.getEffect();
		if (effectType != ModEffects.NOURISHMENT.get() || !MelancholicConfig.disableHunger()) {
			return effect;
		}
		int nourishmentHealthBoostHeartsCount = MelancholicConfig.nourishmentHealthBoostHeartsCount();
		int amplifier;
		if (nourishmentHealthBoostHeartsCount > 0) {
			effectType = NOURISHMENT_HEALTH_BOOST.get();
			amplifier = nourishmentHealthBoostHeartsCount - 1;
		}
		else {
			amplifier = effect.getAmplifier();
		}
		return new MobEffectInstance(
				effectType, effect.getDuration(), amplifier, effect.isAmbient(), false, true
		);
	}

	public static boolean isNourishmentHealthBoost(MobEffect effect) {
		return effect == NOURISHMENT_HEALTH_BOOST.get();
	}

	public static MobEffect getEffectForSprite(MobEffect effect) {
		// making nourishment health boost effect use standard nourishment's sprite
		if (isNourishmentHealthBoost(effect)) {
			return ModEffects.NOURISHMENT.get();
		}
		return effect;
	}

	public static boolean playerHasEffect(Player player) {
		return player.hasEffect(ModEffects.NOURISHMENT.get()) || player.hasEffect(NOURISHMENT_HEALTH_BOOST.get());
	}

	public static void register(IEventBus modBus) {
		MOB_EFFECTS.register(modBus);
	}
}
