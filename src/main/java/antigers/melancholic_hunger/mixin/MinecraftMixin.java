package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.ResourcesReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private ReloadableResourceManager resourceManager;
    @Unique
    private final ResourcesReloadListener resourcesReloadListener = new ResourcesReloadListener();

    @Inject(
            method="onGameLoadFinished",
            at=@At("TAIL")
    )
    private void melancholic_hunger$onFinishedLoading(CallbackInfo ci) {
        resourcesReloadListener.onResourceManagerReload(resourceManager);
    }

    @Inject(
        method="<init>",
        at=@At("TAIL")
    )
    private void melancholic_hunger$registerResourcesReloadListener(GameConfig gameConfig, CallbackInfo ci) {
        resourceManager.registerReloadListener(resourcesReloadListener);
    }
}
