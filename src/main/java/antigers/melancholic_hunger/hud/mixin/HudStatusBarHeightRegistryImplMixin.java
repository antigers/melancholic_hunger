package antigers.melancholic_hunger.hud.mixin;

import antigers.melancholic_hunger.ModLoader;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.impl.client.rendering.hud.HudStatusBarHeightRegistryImpl;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HudStatusBarHeightRegistryImpl.class)
public class HudStatusBarHeightRegistryImplMixin {
    @WrapOperation(
            method = "getHeight",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/impl/client/rendering/hud/HudStatusBarHeightRegistryImpl$ResolvedHeightProvider;getResolvedHeight(Lnet/minecraft/world/entity/player/Player;)I"
            )
    )
    private static int melancholic_hunger$getGuiElementsHeight(
            HudStatusBarHeightRegistryImpl.ResolvedHeightProvider instance, Player player, Operation<Integer> original
    ) {
        // making all gui layers follow our offset
        return original.call(instance, player) + ModLoader.getHudOffset();
    }
}
