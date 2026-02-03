package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import antigers.melancholic_hunger.components.ServerConfigComponent;
import antigers.melancholic_hunger.utils.ClientOnlyHelper;
import mod.adrenix.nostalgic.config.cache.ConfigCache;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfigCache.class)
public class ConfigCacheMixin {

    @Inject(method="save", at=@At("TAIL"), remap=false)
    private static void melancholic_hunger$save(CallbackInfo ci) {
        // Our ConfigHandlerMixin syncs received NT config changes to Melancholic config, however we still need to
        // sync these changes to all other players, so their Melancholic config also updates
        if (FMLEnvironment.dist.isDedicatedServer()) {
            ServerConfigComponent.syncAllPlayers();
        }
        else if (ClientOnlyHelper.hasSingleplayerServer()) {
            ServerConfigComponent.syncAllPlayersExceptOf(ClientOnlyHelper.getLocalPlayerId());
        }
    }
}
