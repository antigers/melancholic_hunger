package antigers.melancholic_hunger.components;

import java.util.HashSet;

import antigers.melancholic_hunger.config.YACLConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class HealthRegenerationComponent implements AutoSyncedComponent, ServerTickingComponent {

    private static class ConsumedFood {
        private final int foodComponentId;
        private final int foodNutrition;
        private int digestedNutrition = 0;
        private int ticksCounter = 0;
        private final int ticksToHeal;

        ConsumedFood (FoodProperties foodComponent, int foodNutrition) {
            this.foodComponentId = foodComponent.hashCode();
            this.foodNutrition = foodNutrition;
            this.ticksToHeal = Math.max(
                    1, (int)(foodNutrition * 20 / (foodComponent.saturation() * YACLConfig.gradualHealthRegenerationSpeed()))
            );
        }

        int getFoodComponentId() {
            return foodComponentId;
        }

        boolean isFullyDigested() {
            return digestedNutrition >= foodNutrition;
        }

        boolean tick() {
            if (ticksCounter < ticksToHeal) {
                ticksCounter++;
                return false;
            }
            digestedNutrition++;
            ticksCounter = 0;
            return true;
        }
    }

    private final TypeToken<HashSet<ConsumedFood>> consumedFoodSetTypeToken = new TypeToken<>() {};

    private final Player player;
    private HashSet<ConsumedFood> consumedFoods = new HashSet<>();
    private int consumedNutrition = 0;
    private final Gson gson = new Gson();

    public HealthRegenerationComponent(Player player) {
        this.player = player;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.consumedNutrition = Math.max(tag.getInt("consumedNutrition"), 0);
        var consumedFoodsStr = tag.getString("consumedFoods");
        if (!consumedFoodsStr.isEmpty()) {
            this.consumedFoods = gson.fromJson(consumedFoodsStr, consumedFoodSetTypeToken);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("consumedNutrition", this.consumedNutrition);
        tag.putString("consumedFoods", gson.toJson(this.consumedFoods));
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return player == this.player; // only sync with the provider itself
    }

    @Override
    public void serverTick() {
        if (!YACLConfig.gradualHealthRegeneration()) {
            return;
        }
        if (consumedFoods.isEmpty()) {
            if (consumedNutrition != 0) {
                consumedNutrition = 0;
                sync();
            }
            return;
        }
        var digestingFoods = new HashSet<Integer>();
        for (var iterator = consumedFoods.iterator(); iterator.hasNext();) {
            var consumedFood = iterator.next();
            var consumedFoodId = consumedFood.getFoodComponentId();
            if (digestingFoods.contains(consumedFoodId)) {
                // parallel healing only with unique food types
                continue;
            }
            digestingFoods.add(consumedFoodId);
            if (!consumedFood.tick()) {
                continue;
            }
            if (consumedNutrition > 0) {
                player.heal(1.0F);
                consumedNutrition--;
            }
            if (consumedFood.isFullyDigested()) {
                iterator.remove();
            }
        }
        sync();
    }

    private void sync() {
        PlayerComponents.HEALTH_REGENERATION.sync(player);
    }

    public boolean canEat() {
        if (!YACLConfig.disableHunger()) {
            return player.getFoodData().needsFood();
        }
        if (!YACLConfig.gradualHealthRegeneration()) {
            return player.getHealth() < player.getMaxHealth();
        }
        return player.getHealth() + consumedNutrition < player.getMaxHealth();
    }

    public void eat(ItemStack itemStack, FoodProperties foodComponent) {
        if (!YACLConfig.disableHunger()) {
            return;
        }
        var foodHealth = YACLConfig.getFoodHealth(itemStack, foodComponent);
        if (YACLConfig.gradualHealthRegeneration()) {
            consumedNutrition += foodHealth;
            consumedFoods.add(new ConsumedFood(foodComponent, foodHealth));
            sync();
        }
        else {
            player.heal(foodHealth);
        }
    }

    public int getConsumedNutrition() {
        if (!YACLConfig.gradualHealthRegeneration()) {
            return 0;
        }
        return consumedNutrition;
    }
}
