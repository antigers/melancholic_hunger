package antigers.melancholic_hunger.compat.farmers_delight.mixin;

import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {
    @WrapOperation(
            method = "renderIcons",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/MobEffectTextureManager;get(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
            )
    )
    private TextureAtlasSprite melancholic_hunger$getInventoryEffectSprite(MobEffectTextureManager instance, Holder<MobEffect> effect, Operation<TextureAtlasSprite> original) {
        return original.call(instance, NourishmentEffectHandler.getEffectForSprite(effect));
    }

    @WrapOperation(
            method = "getEffectName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I",
                    ordinal = 0
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
