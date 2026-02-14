package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractInventoryScreen.class)
public class AbstractInventoryScreenMixin {
	@WrapOperation(
			method="drawStatusEffectSprites",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/client/texture/StatusEffectSpriteManager;getSprite(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/client/texture/Sprite;"
			)
	)
	private Sprite melancholic_hunger$getInventoryEffectSprite(StatusEffectSpriteManager instance, RegistryEntry<StatusEffect> effect, Operation<Sprite> original) {
		return original.call(instance, NourishmentEffectHandler.getEffectForSprite(effect));
	}

	@WrapOperation(
			method="getStatusEffectDescription",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/entity/effect/StatusEffectInstance;getAmplifier()I",
					ordinal=0
			)
	)
	private int melancholic_hunger$getEffectDescription(StatusEffectInstance effect, Operation<Integer> original) {
		// removing drawn effect amplifier number for nourishment health boost effect
		if (NourishmentEffectHandler.isNourishmentHealthBoost(effect.getEffectType())) {
			return 0;
		}
		return original.call(effect);
	}
}
