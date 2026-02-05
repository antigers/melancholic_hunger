package antigers.melancholic_hunger.compat.farmers_delight;

import vectorwing.farmersdelight.client.gui.HUDOverlays;

public class HUDHelper {
	public static void setOffset(int offset) {
		HUDOverlays.healthIconsOffset = 32 + offset;
		HUDOverlays.foodIconsOffset = 32 + offset;
	}
}
