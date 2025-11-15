package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.components.PlayerComponents;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MelancholicHunger.MOD_ID)
public class MelancholicHunger {
    public static boolean nostalgicTweaksInstalled = false;
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "melancholic_hunger";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MelancholicHunger(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Registering custom player data components
        PlayerComponents.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        YACLConfig.loadFromDisk();
        nostalgicTweaksInstalled = ModList.get().isLoaded("nostalgic_tweaks");
    }
}