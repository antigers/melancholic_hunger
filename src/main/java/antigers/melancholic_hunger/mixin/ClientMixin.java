package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

@Mixin(Minecraft.class)
public abstract class ClientMixin {
    @Unique
    private static final ResourceLocation ARMOR_FULL_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/hud/armor_full.png"
    );

    @Shadow public abstract VanillaPackResources getVanillaPackResources();
    @Shadow public abstract ResourceManager getResourceManager();
    @Shadow public abstract boolean isGameLoadFinished();

    @Inject(
        method="onResourceLoadFinished",
        at=@At("RETURN")
    )
    private void melancholic_hunger$onFinishedLoading(Minecraft.GameLoadCookie loadingContext, CallbackInfo ci) {
        if (!this.isGameLoadFinished()) {
            return;
        }
        var currentArmorResource = this.getResourceManager().getResource(ARMOR_FULL_TEXTURE_PATH);
        boolean isDefaultArmorHudTexture = currentArmorResource
                .map(value -> value.sourcePackId().equals("vanilla"))
                .orElse(false);
        if (isDefaultArmorHudTexture) {
            DrawHudContext.isDefaultArmorHudTexture = true;
            return;
        }
        // getting texture from the default vanilla resource pack
        var vanillaResourceManager = new FallbackResourceManager(PackType.CLIENT_RESOURCES, "minecraft");
        vanillaResourceManager.push(this.getVanillaPackResources());
        var vanillaArmorResource = vanillaResourceManager.getResource(ARMOR_FULL_TEXTURE_PATH);
        try {
            var vanillaTexture = vanillaArmorResource.orElseThrow().open().readAllBytes();
            var currentTexture = currentArmorResource.orElseThrow().open().readAllBytes();
            DrawHudContext.isDefaultArmorHudTexture = Arrays.equals(vanillaTexture, currentTexture);
        }
        catch (IOException | NoSuchElementException ignored) {
            DrawHudContext.isDefaultArmorHudTexture = false;
        }
    }
}
