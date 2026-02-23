package antigers.melancholic_hunger.food;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class FoodPropertiesAdder {
	int nutrition = 0;
	float saturation = 0F;

	public void add(FoodProperties foodProperties, int multiplier) {
		nutrition += foodProperties.nutrition() * multiplier;
		saturation += foodProperties.saturation() * multiplier;
	}

	public void addFromItem(ItemStack item, int multiplier) {
		FoodProperties foodProperties = item.get(DataComponents.FOOD);
		add(foodProperties, multiplier);
	}

	public FoodProperties getResult() {
		float saturationModifier = saturation / (nutrition * 2.0F);
		return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationModifier).build();
	}
}
