package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.ConfigNetworkHandler;
import antigers.melancholic_hunger.compat.farmers_delight.FarmersDelightCompatRegistrator;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.components.Components;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MelancholicHunger.MOD_ID)
public class MelancholicHunger {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "melancholic_hunger";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MelancholicHunger(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        Components.register(modEventBus);
        ConfigNetworkHandler.register(modEventBus);
		FarmersDelightCompatRegistrator.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        YACLConfig.loadFromDisk();
    }
}