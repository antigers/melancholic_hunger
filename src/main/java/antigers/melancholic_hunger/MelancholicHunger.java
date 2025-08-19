package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MelancholicHunger implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("melancholic_hunger");
	public static boolean nostalgicTweaksInstalled = false;
    public static boolean raisedInstalled = false;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		YACLConfig.loadFromDisk();
		nostalgicTweaksInstalled = FabricLoader.getInstance().getModContainer("nostalgic_tweaks").isPresent();
        raisedInstalled = FabricLoader.getInstance().getModContainer("raised").isPresent();
	}
}