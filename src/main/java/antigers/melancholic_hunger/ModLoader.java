package antigers.melancholic_hunger;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModLoader {
	public static boolean isClientside() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	public static boolean isServerside() {
		return !isClientside();
	}

	public static Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static boolean isModLoaded(String modName) {
		return FabricLoader.getInstance().getModContainer(modName).isPresent();
	}

	public static boolean isModLoading(String modName) {
		return isModLoaded(modName);
	}
}
