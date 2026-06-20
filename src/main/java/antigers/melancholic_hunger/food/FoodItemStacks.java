package antigers.melancholic_hunger.food;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

import java.util.Map;

public class FoodItemStacks {
    private static void modifyMaxStackSizeComponent(DefaultItemComponentEvents.ModifyContext context) {
        Map<Item, Integer> maxStackSizes = MelancholicConfig.getItemStackSizes();
        if (maxStackSizes == null) {
            return;
        }
        context.modify(
                maxStackSizes::containsKey,
                (builder, item) -> {
                    builder.set(DataComponents.MAX_STACK_SIZE, maxStackSizes.get(item));
                }
        );
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(FoodItemStacks::modifyMaxStackSizeComponent);
    }
}
