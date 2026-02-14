package antigers.melancholic_hunger;

import net.neoforged.fml.ModList;

public class InstalledMods {
	public static boolean NOSTALGIC_TWEAKS = checkIfInstalled("nostalgic_tweaks");
	public static boolean RAISED = checkIfInstalled("raised");
	public static boolean FARMERS_DELIGHT = checkIfInstalled("farmersdelight");

	private static boolean checkIfInstalled(String modName) {
		return ModList.get().isLoaded(modName);
	}
}
