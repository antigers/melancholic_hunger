package antigers.melancholic_hunger;

import antigers.melancholic_hunger.components.Components;
import antigers.melancholic_hunger.config.ConfigNetworkHandler;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.tooltip.FoodItemTooltips;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MelancholicHunger implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MOD_ID = "melancholic_hunger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean nostalgicTweaksInstalled = false;
    public static boolean raisedInstalled = false;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		YACLConfig.loadFromDisk();
		nostalgicTweaksInstalled = FabricLoader.getInstance().getModContainer("nostalgic_tweaks").isPresent();
        raisedInstalled = FabricLoader.getInstance().getModContainer("raised").isPresent();
		if (ModLoader.isClientside()) {
			FoodItemTooltips.register();
		}
		Components.register();
		ConfigNetworkHandler.register();
	}
}