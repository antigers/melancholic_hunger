package antigers.melancholic_hunger;

import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.compat.farmers_delight.FarmersDelightCompatRegistrator;
import antigers.melancholic_hunger.config.YACLConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.raphimc.immediatelyfast.ImmediatelyFast;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MelancholicHunger.MOD_ID)
public class MelancholicHunger
{
	// Define mod id in a common place for everything to reference
	public static final String MOD_ID = "melancholic_hunger";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();

	public MelancholicHunger(FMLJavaModLoadingContext context)
	{
		IEventBus modEventBus = context.getModEventBus();

		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		// Register YACL config screen
		context.registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(
						(mc, parent) -> YACLConfig.getYACLInstance().generateScreen(parent)
				)
		);

		// Registering custom player data components
		PlayerComponents.register();
		FarmersDelightCompatRegistrator.register(modEventBus);
	}

	private void commonSetup(final FMLCommonSetupEvent event)
	{
		YACLConfig.loadFromDisk();
	}

	// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
	@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents
	{
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event)
		{
			FoodItemTooltips.register();
			// Disables hud_batching in Immediately Fast, because it breaks hearts rendering
			if (ModList.get().isLoaded("immediatelyfast")) {
				ImmediatelyFast.config.hud_batching = false;
				ImmediatelyFast.runtimeConfig.hud_batching = false;
			}
		}
	}
}
