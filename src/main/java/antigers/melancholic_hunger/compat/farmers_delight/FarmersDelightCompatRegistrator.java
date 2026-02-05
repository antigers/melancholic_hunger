package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.InstalledMods;

public class FarmersDelightCompatRegistrator {
	public static void register() {
		if (InstalledMods.FARMERS_DELIGHT) {
			NourishmentEffectHandler.register();
		}
	}
}