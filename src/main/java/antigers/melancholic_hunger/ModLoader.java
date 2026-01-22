package antigers.melancholic_hunger;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

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
}
