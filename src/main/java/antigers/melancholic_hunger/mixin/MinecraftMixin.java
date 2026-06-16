package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.RestoredHeartsDrawHelper;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private static final ResourceLocation ARMOR_FULL_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/hud/armor_full.png"
    );
	@Unique
	private static final ResourceLocation HEART_FULL_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/full.png"
	);
	@Unique
	private static final ResourceLocation HEART_HALF_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/half.png"
	);
	@Unique
	private static final ResourceLocation HEART_FULL_BLINKING_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/full_blinking.png"
	);

    @Shadow public abstract VanillaPackResources getVanillaPackResources();
    @Shadow public abstract ResourceManager getResourceManager();
    @Shadow public abstract boolean isGameLoadFinished();
    @Shadow public abstract TextureManager getTextureManager();

    @Unique
    private void melancholic_hunger$checkIfDefaultArmorTexture() {
        Optional<Resource> currentArmorResource = this.getResourceManager().getResource(ARMOR_FULL_TEXTURE_PATH);
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
        Optional<Resource> vanillaArmorResource = vanillaResourceManager.getResource(ARMOR_FULL_TEXTURE_PATH);
        try {
            byte[] vanillaTexture = vanillaArmorResource.orElseThrow().open().readAllBytes();
            byte[] currentTexture = currentArmorResource.orElseThrow().open().readAllBytes();
            DrawHudContext.isDefaultArmorHudTexture = Arrays.equals(vanillaTexture, currentTexture);
        }
        catch (IOException | NoSuchElementException ignored) {
            DrawHudContext.isDefaultArmorHudTexture = false;
        }
    }

	@FunctionalInterface
	interface ImagePixelColorChecker {
		boolean shouldBeOpaque(int abgr, int x, int y);
	}

	protected boolean checkIfPixelOpaque(int abgr) {
		int alpha = (abgr >> 24) & 0xFF;
		return alpha > 0;
	}

	private class RightHalfHeartPixelColorChecker implements ImagePixelColorChecker, AutoCloseable {
		NativeImage halfTexture;

		private RightHalfHeartPixelColorChecker() throws IOException {
			Optional<Resource> heartResource = getResourceManager().getResource(HEART_HALF_TEXTURE_PATH);
			try (InputStream stream = heartResource.orElseThrow().open()) {
				halfTexture = NativeImage.read(stream);
			}
		}

		@Override
		public boolean shouldBeOpaque(int abgr, int x, int y) {
			// full heart pixel is opaque and half heart pixel is transparent - this pixel only exists on the right half
			return checkIfPixelOpaque(abgr) && !checkIfPixelOpaque(halfTexture.getPixelRGBA(x, y));
		}

		@Override
		public void close() {
			halfTexture.close();
		}
	}

    @Unique
    private void melancholic_hunger$generateWhiteHeartTexture(
			ResourceLocation inputTexture, ResourceLocation outputTexture, boolean overwriteWithWhite, ImagePixelColorChecker checker
	) throws IOException {
        Optional<Resource> heartResource = this.getResourceManager().getResource(inputTexture);
		try (InputStream stream = heartResource.orElseThrow().open(); NativeImage image = NativeImage.read(stream)) {
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int abgr = image.getPixelRGBA(x, y);
					int color;
					if (!checker.shouldBeOpaque(abgr, x, y))
						color = 0;  // fully transparent
					else if (overwriteWithWhite)
						color = -1;  // fully opaque white
					else
						color = abgr;

					image.setPixelRGBA(x, y, color);
				}
			}
			// Register the newly created texture into the texture manager
			DynamicTexture dynamicTexture = new DynamicTexture(image);
			this.getTextureManager().register(outputTexture, dynamicTexture);
		}
    }

    @Inject(
            method="onResourceLoadFinished",
            at=@At("RETURN")
    )
    private void melancholic_hunger$onFinishedLoading(Minecraft.GameLoadCookie loadingContext, CallbackInfo ci) throws IOException {
        if (!this.isGameLoadFinished()) {
            return;
        }
        melancholic_hunger$checkIfDefaultArmorTexture();
        melancholic_hunger$generateWhiteHeartTexture(
				HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_FULL_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
        melancholic_hunger$generateWhiteHeartTexture(
				HEART_HALF_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_HALF_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
		try (var checker = new RightHalfHeartPixelColorChecker()) {
			melancholic_hunger$generateWhiteHeartTexture(HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_RIGHT_HALF_HEART_TEXTURE, true, checker);
			melancholic_hunger$generateWhiteHeartTexture(HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.ORIGINAL_RIGHT_HALF_HEART_TEXTURE, false, checker);
			melancholic_hunger$generateWhiteHeartTexture(HEART_FULL_BLINKING_TEXTURE_PATH, RestoredHeartsDrawHelper.BLINKING_RIGHT_HALF_HEART_TEXTURE, false, checker);
		}
    }
}
