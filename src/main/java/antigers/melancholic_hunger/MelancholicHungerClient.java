package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.raphimc.immediatelyfast.ImmediatelyFast;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MelancholicHunger.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = MelancholicHunger.MOD_ID, value = Dist.CLIENT)
public class MelancholicHungerClient {
    public MelancholicHungerClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (client, parent) -> YACLConfig.getYACLInstance().generateScreen(parent)
        );
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        FoodItemTooltips.register();
        // Disables hud_batching in Immediately Fast, because it breaks hearts rendering
        if (ModList.get().isLoaded("immediatelyfast")) {
            ImmediatelyFast.config.hud_batching = false;
            ImmediatelyFast.runtimeConfig.hud_batching = false;
        }
    }
}
