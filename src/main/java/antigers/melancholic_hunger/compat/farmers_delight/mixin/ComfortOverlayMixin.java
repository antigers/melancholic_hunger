package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.ComfortEffectHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

@Mixin(HUDOverlays.ComfortOverlay.class)
public class ComfortOverlayMixin {
	@WrapOperation(
			method="render",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraft/entity/player/HungerManager;getSaturationLevel()F"
			)
	)
	private float melancholic_hunger$comfortEffectHUDSaturationCheck(
			HungerManager instance, Operation<Float> original, @Local(name = "player") PlayerEntity player
	) {
		return ComfortEffectHandler.shouldApply(player) ? 0.0F : 1.0F;
	}
}
