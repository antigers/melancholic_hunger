package antigers.melancholic_hunger.hud.mixin;

import net.minecraft.client.GameLoadCookie;
import antigers.melancholic_hunger.hud.TextureHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow public abstract boolean isGameLoadFinished();

    @Inject(
            method="onResourceLoadFinished",
            at=@At("RETURN")
    )
    private void melancholic_hunger$onFinishedLoading(GameLoadCookie loadingContext, CallbackInfo ci) throws IOException {
        if (!this.isGameLoadFinished()) {
            return;
        }
        TextureHelper textureHelper = new TextureHelper();
        textureHelper.checkIfDefaultArmorTexture();
        textureHelper.generateHeartTextures();
    }
}
