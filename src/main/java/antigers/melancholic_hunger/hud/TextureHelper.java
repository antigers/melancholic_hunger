package antigers.melancholic_hunger.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

public class TextureHelper {
	private static final ResourceLocation ARMOR_FULL_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/armor_full.png"
	);
	private static final ResourceLocation HEART_FULL_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/full.png"
	);
	private static final ResourceLocation HEART_HALF_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/half.png"
	);
	private static final ResourceLocation HEART_FULL_BLINKING_TEXTURE_PATH = ResourceLocation.withDefaultNamespace(
			"textures/gui/sprites/hud/heart/full_blinking.png"
	);

	private Minecraft minecraft;

	public TextureHelper() {
		minecraft =  Minecraft.getInstance();
	}

	public void checkIfDefaultArmorTexture() {
		Optional<Resource> currentArmorResource = minecraft.getResourceManager().getResource(ARMOR_FULL_TEXTURE_PATH);
		boolean isDefaultArmorHudTexture = currentArmorResource
				.map(value -> value.sourcePackId().equals("vanilla"))
				.orElse(false);
		if (isDefaultArmorHudTexture) {
			DrawHudContext.isDefaultArmorHudTexture = true;
			return;
		}
		// getting texture from the default vanilla resource pack
		var vanillaResourceManager = new FallbackResourceManager(PackType.CLIENT_RESOURCES, "minecraft");
		vanillaResourceManager.push(minecraft.getVanillaPackResources());
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
	private interface ImagePixelColorChecker {
		boolean shouldBeOpaque(int abgr, int x, int y);
	}

	protected boolean checkIfPixelOpaque(int abgr) {
		int alpha = (abgr >> 24) & 0xFF;
		return alpha > 0;
	}

	private class RightHalfHeartPixelColorChecker implements ImagePixelColorChecker, AutoCloseable {
		NativeImage halfTexture;

		private RightHalfHeartPixelColorChecker() throws IOException {
			Optional<Resource> heartResource = minecraft.getResourceManager().getResource(HEART_HALF_TEXTURE_PATH);
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

	private void generateHeartTexture(
			ResourceLocation inputTexture, ResourceLocation outputTexture, boolean overwriteWithWhite, ImagePixelColorChecker checker
	) throws IOException {
		Optional<Resource> heartResource = minecraft.getResourceManager().getResource(inputTexture);
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
			minecraft.getTextureManager().register(outputTexture, dynamicTexture);
		}
	}

	public void generateHeartTextures() throws IOException {
		generateHeartTexture(
				HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_FULL_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
		generateHeartTexture(
				HEART_HALF_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_HALF_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
		try (var checker = new RightHalfHeartPixelColorChecker()) {
			generateHeartTexture(HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.WHITE_RIGHT_HALF_HEART_TEXTURE, true, checker);
			generateHeartTexture(HEART_FULL_TEXTURE_PATH, RestoredHeartsDrawHelper.ORIGINAL_RIGHT_HALF_HEART_TEXTURE, false, checker);
			generateHeartTexture(HEART_FULL_BLINKING_TEXTURE_PATH, RestoredHeartsDrawHelper.BLINKING_RIGHT_HALF_HEART_TEXTURE, false, checker);
		}
	}
}
