package antigers.melancholic_hunger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.nio.file.Path;

public class ModLoader {
	public static boolean isClientside() {
		return FMLEnvironment.getDist().isClient();
	}

	public static boolean isServerside() {
		return !isClientside();
	}

	public static Path getConfigDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static boolean isModLoaded(String modName) {
		return ModList.get().isLoaded(modName);
	}

	public static boolean isModLoading(String modName) {
		return FMLLoader.getCurrent().getLoadingModList()
				.getMods()
				.stream()
				.map(ModInfo::getModId)
				.anyMatch(id -> id.equals(modName));
	}

	public static void setHudOffset(int offset) {
		Gui gui = Minecraft.getInstance().gui;
		gui.leftHeight += offset;
		gui.rightHeight += offset;
	}
}
