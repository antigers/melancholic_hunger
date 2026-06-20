package antigers.melancholic_hunger.food;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.Map;

public class FoodItemStacks {
    private static void modifyMaxStackSizeComponent(ModifyDefaultComponentsEvent event) {
        Map<Item, Integer> maxStackSizes = MelancholicConfig.getItemStackSizes();
        if (maxStackSizes == null) {
            return;
        }
        event.modifyMatching(
                (item, _) -> maxStackSizes.containsKey(item),
                (builder, _, item) -> {
                    builder.set(DataComponents.MAX_STACK_SIZE, maxStackSizes.get(item));
                }
        );
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FoodItemStacks::modifyMaxStackSizeComponent);
    }
}
