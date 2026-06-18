package antigers.melancholic_hunger;

import net.fabricmc.loader.api.FabricLoader;

public class InstalledMods {
    public static boolean NOSTALGIC_TWEAKS = checkIfInstalled("nostalgic_tweaks");
    public static boolean RAISED = checkIfInstalled("raised");
    public static boolean FARMERS_DELIGHT = checkIfInstalled("farmersdelight");

    private static boolean checkIfInstalled(String modName) {
        return FabricLoader.getInstance().getModContainer(modName).isPresent();
    }
}
