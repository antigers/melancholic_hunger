package antigers.melancholic_hunger.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
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
	private Minecraft minecraft;

	public TextureHelper() {
		minecraft =  Minecraft.getInstance();
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
			Optional<Resource> heartResource = minecraft.getResourceManager().getResource(Gui.GUI_ICONS_LOCATION);
			try (InputStream stream = heartResource.orElseThrow().open()) {
				halfTexture = NativeImage.read(stream);
			}
		}

		@Override
		public boolean shouldBeOpaque(int abgr, int x, int y) {
			// full heart pixel is opaque and half heart pixel is transparent - this pixel only exists on the right half
			return checkIfPixelOpaque(abgr) && !checkIfPixelOpaque(halfTexture.getPixelRGBA(x + 9, y));
		}

		@Override
		public void close() {
			halfTexture.close();
		}
	}

	private void generateHeartTexture(
			int inputTextureX, ResourceLocation outputTexture, boolean overwriteWithWhite, ImagePixelColorChecker checker
	) throws IOException {
		Optional<Resource> heartResource = minecraft.getResourceManager().getResource(Gui.GUI_ICONS_LOCATION);
		try (
				InputStream stream = heartResource.orElseThrow().open();
				NativeImage image = NativeImage.read(stream);
				NativeImage outputImage = new NativeImage(9, 9, false)
		) {
			for (int y = 0; y < image.getHeight(); y++) {
				if (y >= 9)
					break;

				int outputX = 0;
				for (int x = inputTextureX; x < inputTextureX + 9; x++) {
					int abgr = image.getPixelRGBA(x, y);
					int color;
					if (!checker.shouldBeOpaque(abgr, x, y))
						color = 0;  // fully transparent
					else if (overwriteWithWhite)
						color = -1;  // fully opaque white
					else
						color = abgr;

					outputImage.setPixelRGBA(outputX, y, color);
					outputX++;
				}
			}
			// Register the newly created texture into the texture manager
			DynamicTexture dynamicTexture = new DynamicTexture(outputImage);
			minecraft.getTextureManager().register(outputTexture, dynamicTexture);
		}
	}

	public void generateHeartTextures() throws IOException {
		int heartFullTextureX = Gui.HeartType.NORMAL.getX(false, false);
		int heartHalfTextureX = Gui.HeartType.NORMAL.getX(true, false);
		int heartFullBlinkingTextureX = Gui.HeartType.NORMAL.getX(false, true);
		generateHeartTexture(
				heartFullTextureX, RestoredHeartsDrawHelper.WHITE_FULL_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
		generateHeartTexture(
				heartHalfTextureX, RestoredHeartsDrawHelper.WHITE_HALF_HEART_TEXTURE, true, (abgr, x, y) -> checkIfPixelOpaque(abgr)
		);
		try (var checker = new RightHalfHeartPixelColorChecker()) {
			generateHeartTexture(heartFullTextureX, RestoredHeartsDrawHelper.WHITE_RIGHT_HALF_HEART_TEXTURE, true, checker);
			generateHeartTexture(heartFullTextureX, RestoredHeartsDrawHelper.ORIGINAL_RIGHT_HALF_HEART_TEXTURE, false, checker);
			generateHeartTexture(heartFullBlinkingTextureX, RestoredHeartsDrawHelper.BLINKING_RIGHT_HALF_HEART_TEXTURE, false, checker);
		}
	}
}
