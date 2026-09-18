package antigers.melancholic_hunger;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class ModLoader {
    private static int hudOffset = 0;
    private static MinecraftServer serverInstance = null;

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

    public static void setHudOffset(int offset) {
        hudOffset = offset;
    }

    public static int getHudOffset() {
        return hudOffset;
    }

    public static void setServerInstance(MinecraftServer server) {
        serverInstance = server;
    }

    public static MinecraftServer getServerInstance() {
        return serverInstance;
    }
}
