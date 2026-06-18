package antigers.melancholic_hunger.compat.farmers_delight;

import antigers.melancholic_hunger.food.FoodPropertiesAdder;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
//import vectorwing.farmersdelight.common.block.FeastBlock;
//import vectorwing.farmersdelight.common.block.PieBlock;
//import vectorwing.farmersdelight.common.block.RiceRollMedleyBlock;

import java.util.Optional;

public class FarmersDelightEdibleBlockFoods {

    public static Optional<FoodProperties> getFoodProperties(BlockItem blockItem) {
        Block block = blockItem.getBlock();
        FoodPropertiesAdder totalFoodProperties = new FoodPropertiesAdder();
        switch (block) {
//			case PieBlock pieBlock -> totalFoodProperties.addFromItem(pieBlock.getPieSliceItem(), pieBlock.getMaxBites());
//			case RiceRollMedleyBlock riceRollMedleyBlock ->
//					riceRollMedleyBlock.riceRollServings.get().forEach(item -> totalFoodProperties.addFromItem(item.getDefaultInstance(), 1));
//			case FeastBlock feastBlock ->
//					totalFoodProperties.addFromItem(feastBlock.servingItem.get().getDefaultInstance(), feastBlock.getMaxServings());
            default -> {
                return Optional.empty();
            }
        }
//		return Optional.of(totalFoodProperties.getResult());
    }
}
