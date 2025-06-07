package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

@Mixin(MinecraftClient.class)
public abstract class ClientMixin {
    @Unique
    private static final Identifier ARMOR_FULL_TEXTURE_PATH = Identifier.ofVanilla(
            "textures/gui/sprites/hud/armor_full.png"
    );

    @Shadow public abstract DefaultResourcePack getDefaultResourcePack();
    @Shadow public abstract ResourceManager getResourceManager();
    @Shadow public abstract boolean isFinishedLoading();

    @Inject(
        method="onFinishedLoading",
        at=@At("RETURN")
    )
    private void melancholic_hunger$onFinishedLoading(MinecraftClient.LoadingContext loadingContext, CallbackInfo ci) {
        if (!this.isFinishedLoading()) {
            return;
        }
        var currentArmorResource = this.getResourceManager().getResource(ARMOR_FULL_TEXTURE_PATH);
        boolean isDefaultArmorHudTexture = currentArmorResource
                .map(value -> value.getPackId().equals("vanilla"))
                .orElse(false);
        if (isDefaultArmorHudTexture) {
            DrawHudContext.isDefaultArmorHudTexture = true;
            return;
        }
        // getting texture from the default vanilla resource pack
        var vanillaResourceManager = new NamespaceResourceManager(ResourceType.CLIENT_RESOURCES, "minecraft");
        vanillaResourceManager.addPack(this.getDefaultResourcePack());
        var vanillaArmorResource = vanillaResourceManager.getResource(ARMOR_FULL_TEXTURE_PATH);
        try {
            var vanillaTexture = vanillaArmorResource.orElseThrow().getInputStream().readAllBytes();
            var currentTexture = currentArmorResource.orElseThrow().getInputStream().readAllBytes();
            DrawHudContext.isDefaultArmorHudTexture = Arrays.equals(vanillaTexture, currentTexture);
        }
        catch (IOException | NoSuchElementException ignored) {
            DrawHudContext.isDefaultArmorHudTexture = false;
        }
    }
}
