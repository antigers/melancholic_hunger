package antigers.melancholic_hunger.tooltip;

import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.food.EdibleBlockFoods;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FoodItemTooltips {
	private static final String CONFIG_PREFIX = "gui.melancholic_hunger.regeneration_tooltip.";

	public record FoodHealthTextComponent(int foodNutrition) implements Component, FormattedCharSequence {
		@Override
		public Style getStyle() {
			return Style.EMPTY;
		}

		@Override
		public ComponentContents getContents() {
			return PlainTextContents.EMPTY;
		}

		static List<Component> emptySiblings = new ArrayList<>();

		@Override
		public List<Component> getSiblings() {
			return emptySiblings;
		}

		@Override
		public FormattedCharSequence getVisualOrderText() {
			return this;
		}

		@Override
		public boolean accept(FormattedCharSink visitor) {
			return StringDecomposer.iterateFormatted(this, getStyle(), visitor);
		}

		public FoodHealthTooltipComponent getComponent() {
			return FoodHealthTooltipComponent.init(foodNutrition);
		}
	}

	public static class FoodHealthTooltipComponent extends ClientTextTooltip
	{
		private final int heartsCount;
		private final boolean lastHeartIsHalf;

		FoodHealthTooltipComponent(String text, int foodNutrition)
		{
			super(Component.literal(text).getVisualOrderText());
			heartsCount = (int) Math.ceil(foodNutrition / 2f);
			lastHeartIsHalf = foodNutrition % 2 != 0;
		}

		public static FoodHealthTooltipComponent init(int foodNutrition) {
			String text = "";
			if (foodNutrition > 20) {
				text = "x%d".formatted(foodNutrition / 2);
				if (foodNutrition % 2 > 0) {
					text += ".5";
				}
				foodNutrition = 2;
			}
			return new FoodHealthTooltipComponent(text, foodNutrition);
		}

		@Override
		public int getHeight(Font font) {
			return 14;
		}

		@Override
		public int getWidth(Font font)
		{
			return heartsCount * 9 + super.getWidth(font);
		}

		@Override
		public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
			super.extractText(graphics, font, x + 12, y + 2);
		}

		@Override
		public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics)
		{
			y += 2;
			for (int i = 0; i < heartsCount - 1; i++) {
				int textureX = x + i * 9;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.HeartType.CONTAINER.getSprite(false, false, false), textureX, y, 9, 9);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.HeartType.NORMAL.getSprite(false, false, false), textureX, y, 9, 9);
			}
			int textureX = x + (heartsCount - 1) * 9;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.HeartType.CONTAINER.getSprite(false, lastHeartIsHalf, false), textureX, y, 9, 9);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.HeartType.NORMAL.getSprite(false, lastHeartIsHalf, false), textureX, y, 9, 9);
		}
	}

	private static void appendTooltip(ItemStack stack, List<Component> lines) {
		if (!MelancholicConfig.disableHunger() || !MelancholicConfig.showFoodItemTooltips()) {
			return;
		}
		FoodProperties foodProperties = stack.get(DataComponents.FOOD);
		if (foodProperties == null) {
			var foodPropertiesOptional = EdibleBlockFoods.getFoodProperties(stack.getItem());
			if (foodPropertiesOptional.isEmpty()) {
				return;
			}
			foodProperties = foodPropertiesOptional.get();
		}
		int foodNutrition = MelancholicConfig.getFoodHealth(stack, foodProperties);
		if (foodNutrition <= 0) {
			return;
		}
		lines.add(new FoodHealthTextComponent(foodNutrition));
		if (!MelancholicConfig.gradualHealthRegeneration() || !MelancholicConfig.saturationBasedRegeneration()) {
			return;
		}
		float regenerationRatio = foodNutrition / foodProperties.saturation();
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
		lines.add(
				Component.translatable(CONFIG_PREFIX + "template", Component.translatable(regenerationRate))
						.setStyle(Style.EMPTY.withColor(formatting.getColor()))
		);
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register(
				(stack, _, _, lines) -> appendTooltip(stack, lines)
		);
	}
}