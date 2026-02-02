package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MelancholicHunger.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FoodItemTooltips {
	private static final String CONFIG_PREFIX = "gui.melancholic_hunger.regeneration_tooltip.";

	private record FoodHealthTooltipComponentType (int foodNutrition) implements TooltipComponent { }

	private static class FoodHealthTooltipComponent implements ClientTooltipComponent
	{
		private final int heartsCount;
		private final boolean lastHeartIsHalf;

		FoodHealthTooltipComponent(int foodNutrition)
		{
			heartsCount = (int) Math.ceil(foodNutrition / 2f);
			lastHeartIsHalf = foodNutrition % 2 != 0;
		}

		@Override
		public int getHeight()
		{
			return 14;
		}

		@Override
		public int getWidth(Font font)
		{
			return heartsCount * 9;
		}

		@Override
		public void renderImage(Font font, int x, int y, GuiGraphics context)
		{
			y += 2;
			for (int i = 0; i < heartsCount - 1; i++) {
				int textureX = x + i * 9;
				context.blit(Gui.GUI_ICONS_LOCATION, textureX, y, Gui.HeartType.CONTAINER.getX(false, false), 0, 9, 9);
				context.blit(Gui.GUI_ICONS_LOCATION, textureX, y, Gui.HeartType.NORMAL.getX(false, false), 0, 9, 9);
			}
			int textureX = x + (heartsCount - 1) * 9;
			context.blit(Gui.GUI_ICONS_LOCATION, textureX, y, Gui.HeartType.CONTAINER.getX(lastHeartIsHalf, false), 0, 9, 9);
			context.blit(Gui.GUI_ICONS_LOCATION, textureX, y, Gui.HeartType.NORMAL.getX(lastHeartIsHalf, false), 0, 9, 9);
		}
	}

	private static void appendTooltip(RenderTooltipEvent.GatherComponents event) {
		ItemStack stack = event.getItemStack();
		FoodProperties foodProperties = stack.getItem().getFoodProperties();
		if (foodProperties == null) {
			if (stack.getItem() != Items.CAKE) {
				return;
			}
			foodProperties = Cake.FOOD_PROPERTIES;
		}
		if (!YACLConfig.showFoodItemTooltips()) {
			return;
		}
		int foodNutrition = YACLConfig.getFoodHealth(stack, foodProperties);
		if (foodNutrition <= 0) {
			return;
		}
		List<Either<FormattedText, TooltipComponent>> lines = event.getTooltipElements();
		lines.add(Either.right(new FoodHealthTooltipComponentType(foodNutrition)));
		if (!YACLConfig.gradualHealthRegeneration()) {
			return;
		}
		float regenerationRatio = 0.5F / foodProperties.getSaturationModifier();
		String regenerationRate;
		ChatFormatting formatting;
		if (regenerationRatio <= 0.5F) {
			regenerationRate = CONFIG_PREFIX + "super_fast";
			formatting = ChatFormatting.DARK_PURPLE;
		} else if (regenerationRatio <= 0.8F) {
			regenerationRate = CONFIG_PREFIX + "very_fast";
			formatting = ChatFormatting.DARK_GREEN;
		} else if (regenerationRatio <= 1.6F) {
			regenerationRate = CONFIG_PREFIX + "fast";
			formatting = ChatFormatting.GREEN;
		} else if (regenerationRatio <= 2.5F) {
			regenerationRate = CONFIG_PREFIX + "slow";
			formatting = ChatFormatting.RED;
		} else {
			regenerationRate = CONFIG_PREFIX + "very_slow";
			formatting = ChatFormatting.DARK_RED;
		}
		lines.add(Either.left(
				Component.translatable(CONFIG_PREFIX + "template", Component.translatable(regenerationRate))
						.setStyle(Style.EMPTY.withColor(formatting.getColor()))
		));
	}

	@SubscribeEvent
	public static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(
				FoodHealthTooltipComponentType.class,
				foodHealthTooltipComponentType -> new FoodHealthTooltipComponent(foodHealthTooltipComponentType.foodNutrition())
		);
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addListener(FoodItemTooltips::appendTooltip);
	}
}