package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
	@WrapOperation(
			method="extractEffects",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/Gui;getMobEffectSprite(Lnet/minecraft/core/Holder;)Lnet/minecraft/resources/Identifier;"
			)
	)
	private Identifier melancholic_hunger$getInventoryEffectSprite(Holder<MobEffect> effect, Operation<Identifier> original) {
		return original.call(NourishmentEffectHandler.getEffectForSprite(effect));
	}

	@WrapOperation(
			method="getEffectName",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I",
					ordinal=0
			)
	)
	private int melancholic_hunger$getEffectDescription(MobEffectInstance effect, Operation<Integer> original) {
		// removing drawn effect amplifier number for nourishment health boost effect
		if (NourishmentEffectHandler.isNourishmentHealthBoost(effect.getEffect())) {
			return 0;
		}
		return original.call(effect);
	}
}
