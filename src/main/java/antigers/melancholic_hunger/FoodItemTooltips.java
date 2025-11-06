package antigers.melancholic_hunger;

import antigers.melancholic_hunger.config.YACLConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class FoodItemTooltips {
	private static final String CONFIG_PREFIX = "gui.melancholic_hunger.regeneration_tooltip.";

	public record FoodHealthTextComponent(int foodNutrition) implements Text, OrderedText {
		@Override
		public Style getStyle() {
			return Style.EMPTY;
		}

		@Override
		public TextContent getContent() {
			return PlainTextContent.EMPTY;
		}

		static List<Text> emptySiblings = new ArrayList<>();

		@Override
		public List<Text> getSiblings() {
			return emptySiblings;
		}

		@Override
		public OrderedText asOrderedText() {
			return this;
		}

		@Override
		public boolean accept(CharacterVisitor visitor) {
			return TextVisitFactory.visitFormatted(this, getStyle(), visitor);
		}

		public FoodHealthTooltipComponent getComponent() {
			return new FoodHealthTooltipComponent(foodNutrition);
		}
	}

	public static class FoodHealthTooltipComponent implements TooltipComponent
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
		public int getWidth(TextRenderer textRenderer)
		{
			return heartsCount * 9;
		}

		@Override
		public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context)
		{
			y += 2;
			for (int i = 0; i < heartsCount - 1; i++) {
				int textureX = x + i * 9;
				context.drawGuiTexture(InGameHud.HeartType.CONTAINER.getTexture(false, false, false), textureX, y, 9, 9);
				context.drawGuiTexture(InGameHud.HeartType.NORMAL.getTexture(false, false, false), textureX, y, 9, 9);
			}
			int textureX = x + (heartsCount - 1) * 9;
			context.drawGuiTexture(InGameHud.HeartType.CONTAINER.getTexture(false, lastHeartIsHalf, false), textureX, y, 9, 9);
			context.drawGuiTexture(InGameHud.HeartType.NORMAL.getTexture(false, lastHeartIsHalf, false), textureX, y, 9, 9);
		}
	}

	private static void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipType tooltipType, List<Text> lines) {
		FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
		if (foodComponent == null) {
			if (stack.getItem() != Items.CAKE) {
				return;
			}
			foodComponent = Cake.FOOD_COMPONENT;
		}
		if (!YACLConfig.showFoodItemTooltips()) {
			return;
		}
		int foodNutrition = YACLConfig.getFoodHealth(stack, foodComponent);
		if (foodNutrition <= 0) {
			return;
		}
		lines.add(new FoodHealthTextComponent(foodNutrition));
		if (!YACLConfig.gradualHealthRegeneration()) {
			return;
		}
		float regenerationRatio = foodNutrition / foodComponent.saturation();
		String regenerationRate;
		Formatting formatting;
		if (regenerationRatio <= 0.5F) {
			regenerationRate = CONFIG_PREFIX + "super_fast";
			formatting = Formatting.DARK_PURPLE;
		} else if (regenerationRatio <= 0.8F) {
			regenerationRate = CONFIG_PREFIX + "very_fast";
			formatting = Formatting.DARK_GREEN;
		} else if (regenerationRatio <= 1.6F) {
			regenerationRate = CONFIG_PREFIX + "fast";
			formatting = Formatting.GREEN;
		} else if (regenerationRatio <= 2.5F) {
			regenerationRate = CONFIG_PREFIX + "slow";
			formatting = Formatting.RED;
		} else {
			regenerationRate = CONFIG_PREFIX + "very_slow";
			formatting = Formatting.DARK_RED;
		}
		lines.add(
				Text.translatable(CONFIG_PREFIX + "template", Text.translatable(regenerationRate))
						.setStyle(Style.EMPTY.withColor(formatting.getColorValue()))
		);
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register(FoodItemTooltips::appendTooltip);
	}
}