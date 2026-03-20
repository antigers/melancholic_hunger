package antigers.melancholic_hunger.components;

import java.util.HashSet;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import org.ladysnake.cca.api.v3.util.CheckEnvironment;

public class HealthRegenerationComponent implements AutoSyncedComponent, ServerTickingComponent {

    private static class ConsumedFood {
        private final int foodComponentId;
        private final int foodNutrition;
        private int digestedNutrition = 0;
        private int ticksCounter = 0;
        private final int ticksToHeal;

        ConsumedFood (int foodNutrition, float foodSaturation, int foodComponentId) {
            this.foodComponentId = foodComponentId;
            this.foodNutrition = foodNutrition;
            float ratio = Math.min(5.0F, foodNutrition / foodSaturation);
            this.ticksToHeal = Math.max(
                    1, (int)(ratio * 20 / MelancholicConfig.gradualHealthRegenerationSpeed())
            );
        }

        int getFoodComponentId() {
            return foodComponentId;
        }

        boolean isFullyDigested() {
            return digestedNutrition >= foodNutrition;
        }

        boolean tick(float regenSpeedMultiplier) {
            int ticksToHeal = Math.max(1, (int)(this.ticksToHeal / regenSpeedMultiplier));
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

    public static HealthRegenerationComponent get(Player player) {
        return PlayerComponents.HEALTH_REGENERATION.get(player);
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
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeInt(consumedNutrition);
    }

    @Override
    @CheckEnvironment(EnvType.CLIENT)
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        consumedNutrition = buf.readInt();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return player == this.player; // only sync with the provider itself
    }

    @Override
    public void serverTick() {
        if (!MelancholicConfig.gradualHealthRegeneration()) {
            return;
        }
        if (consumedFoods.isEmpty()) {
            if (consumedNutrition != 0) {
                consumedNutrition = 0;
                sync();
            }
            return;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            // player is at full health
            switch (MelancholicConfig.regenerationAtFullHealth()) {
                case STOPPED -> {
                    consumedFoods.clear();
                    consumedNutrition = 0;
                    sync();
                    return;
                }
                case STORED -> {
                    return;
                }
            }
        }
        var digestingFoods = new HashSet<Integer>();
        boolean needsSync = false;
        float regenSpeedMultiplier = 1.0F;
        if (InstalledMods.FARMERS_DELIGHT && NourishmentEffectHandler.playerHasEffect(player)) {
            regenSpeedMultiplier = MelancholicConfig.nourishmentRegenSpeedMultiplier();
        }
        for (var iterator = consumedFoods.iterator(); iterator.hasNext();) {
            var consumedFood = iterator.next();
            var consumedFoodId = consumedFood.getFoodComponentId();
            if (digestingFoods.contains(consumedFoodId)) {
                // parallel healing only with unique food types
                continue;
            }
            digestingFoods.add(consumedFoodId);
            if (!consumedFood.tick(regenSpeedMultiplier)) {
                continue;
            }
            if (consumedNutrition > 0) {
                player.heal(1.0F);
                consumedNutrition--;
                needsSync = true;
            }
            if (consumedFood.isFullyDigested()) {
                iterator.remove();
            }
        }
        if (needsSync) {
            sync();
        }
    }

    private void sync() {
        PlayerComponents.HEALTH_REGENERATION.sync(player);
    }

    public boolean canEat() {
        if (!MelancholicConfig.disableHunger()) {
            return player.getFoodData().needsFood();
        }
        if (!MelancholicConfig.gradualHealthRegeneration()) {
            return player.getHealth() < player.getMaxHealth();
        }
        return player.getHealth() + consumedNutrition < player.getMaxHealth();
    }

    public void eat(int foodHealth, float foodSaturation, int foodComponentId) {
        if (MelancholicConfig.gradualHealthRegeneration()) {
            consumedNutrition += foodHealth;
            consumedFoods.add(new ConsumedFood(foodHealth, foodSaturation, foodComponentId));
            sync();
        }
        else {
            player.heal(foodHealth);
        }
    }

    public boolean eat(ItemStack itemStack, FoodProperties foodComponent) {
        if (!(player instanceof ServerPlayer) || !MelancholicConfig.disableHunger()) {
            return false;
        }
        var foodHealth = MelancholicConfig.getFoodHealth(itemStack, foodComponent);
        eat(foodHealth, foodComponent.saturation(), foodComponent.hashCode());
        return true;
    }

    public int getConsumedNutrition() {
        if (!MelancholicConfig.gradualHealthRegeneration()) {
            return 0;
        }
        return consumedNutrition;
    }
}
