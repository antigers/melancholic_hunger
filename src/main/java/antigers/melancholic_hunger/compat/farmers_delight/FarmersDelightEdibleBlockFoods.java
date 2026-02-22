package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.food.FoodPropertiesAdder;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import vectorwing.farmersdelight.common.block.FeastBlock;
import vectorwing.farmersdelight.common.block.PieBlock;
import vectorwing.farmersdelight.common.block.RiceRollMedleyBlock;

import java.util.Optional;

public class FarmersDelightEdibleBlockFoods {

	public static Optional<FoodProperties> getFoodProperties(BlockItem blockItem) {
		Block block = blockItem.getBlock();
		FoodPropertiesAdder totalFoodProperties = new FoodPropertiesAdder();
		if (block instanceof PieBlock pieBlock) {
			totalFoodProperties.addFromItem(pieBlock.getPieSliceItem().getItem(), pieBlock.getMaxBites());
		}
		else if (block instanceof RiceRollMedleyBlock riceRollMedleyBlock) {
			riceRollMedleyBlock.riceRollServings.forEach(item -> totalFoodProperties.addFromItem(item.get(), 1));
		}
		else if (block instanceof FeastBlock feastBlock) {
			totalFoodProperties.addFromItem(feastBlock.servingItem.get(), feastBlock.getMaxServings());
		}
		else {
			return Optional.empty();
		}
		return Optional.of(totalFoodProperties.getResult());
	}
}
