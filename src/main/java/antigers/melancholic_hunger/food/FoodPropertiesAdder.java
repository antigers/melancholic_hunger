package antigers.melancholic_hunger.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class FoodPropertiesAdder {
    int nutrition = 0;
    float saturation = 0F;

    public void add(FoodProperties foodProperties, int multiplier) {
        nutrition += foodProperties.getNutrition() * multiplier;
        saturation += foodProperties.getNutrition() * foodProperties.getSaturationModifier() * 2.0F * multiplier;
    }

    public void addFromItem(Item item, int multiplier) {
        FoodProperties foodProperties = item.getFoodProperties();
        add(foodProperties, multiplier);
    }

    public FoodProperties getResult() {
        float saturationModifier = saturation / (nutrition * 2.0F);
        return new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturationModifier).build();
    }
}
