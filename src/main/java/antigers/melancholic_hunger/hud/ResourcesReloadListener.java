package antigers.melancholic_hunger.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class ResourcesReloadListener implements ResourceManagerReloadListener {

	private static ResourceManager getResourceManager(PackResources packResources) {
		var resourceManager = new FallbackResourceManager(PackType.CLIENT_RESOURCES, "minecraft");
		resourceManager.push(packResources);
		return resourceManager;
	}

	private static boolean isProgrammerArtTexture(ResourceManager resourceManager, byte[] currentTexture) throws IOException {
		var programmerArt = resourceManager.listPacks().filter(
				packResources -> packResources.packId().equals("programmer_art")
		).findFirst();
		if (programmerArt.isEmpty()) {
			return false;
		}
		var programmerArtResourceManager = getResourceManager(programmerArt.get());
		var programmerArtResource = programmerArtResourceManager.getResource(Gui.GUI_ICONS_LOCATION);
		var programmerArtTexture = programmerArtResource.orElseThrow().open().readAllBytes();
		return Arrays.equals(programmerArtTexture, currentTexture);
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		var currentArmorResource = resourceManager.getResource(Gui.GUI_ICONS_LOCATION);
		boolean isDefaultArmorHudTexture = currentArmorResource
				.map(value -> value.sourcePackId().equals("vanilla"))
				.orElse(false);
		if (isDefaultArmorHudTexture) {
			DrawHudContext.isDefaultArmorHudTexture = true;
			return;
		}
		// getting texture from the default vanilla resource pack
		var vanillaResourceManager = getResourceManager(Minecraft.getInstance().getVanillaPackResources());
		var vanillaArmorResource = vanillaResourceManager.getResource(Gui.GUI_ICONS_LOCATION);
		try {
			var vanillaTexture = vanillaArmorResource.orElseThrow().open().readAllBytes();
			var currentTexture = currentArmorResource.orElseThrow().open().readAllBytes();
			isDefaultArmorHudTexture = Arrays.equals(vanillaTexture, currentTexture);
			if (!isDefaultArmorHudTexture) {
				DrawHudContext.isDefaultArmorHudTexture = isProgrammerArtTexture(resourceManager, currentTexture);
			}
		}
		catch (IOException | NoSuchElementException ignored) {
			DrawHudContext.isDefaultArmorHudTexture = false;
		}
	}
}
