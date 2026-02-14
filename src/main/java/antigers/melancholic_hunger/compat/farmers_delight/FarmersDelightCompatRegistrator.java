package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.InstalledMods;
import net.neoforged.bus.api.IEventBus;

public class FarmersDelightCompatRegistrator {
	public static void register(IEventBus modBus) {
		if (InstalledMods.FARMERS_DELIGHT) {
			NourishmentEffectHandler.register(modBus);
		}
	}
}