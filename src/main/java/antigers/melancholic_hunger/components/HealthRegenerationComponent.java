package antigers.melancholic_hunger.components;

import java.util.HashSet;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class HealthRegenerationComponent {

    private static class ConsumedFood {
        private final int foodComponentId;
        private final int foodNutrition;
        private int digestedNutrition = 0;
        private int ticksCounter = 0;
        private final int ticksToHeal;

        ConsumedFood (int foodNutrition, float foodSaturation, int foodComponentId) {
            this.foodComponentId = foodComponentId;
            this.foodNutrition = foodNutrition;
            if (MelancholicConfig.saturationBasedRegeneration()) {
                float ratio = Math.min(5.0F, foodNutrition / foodSaturation);
                this.ticksToHeal = Math.max(
                        1, (int) (ratio * 20 / MelancholicConfig.gradualHealthRegenerationSpeed())
                );
            }
            else {
                this.ticksToHeal = (int) (20 / MelancholicConfig.gradualHealthRegenerationSpeed());
            }
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

    private static final TypeToken<HashSet<ConsumedFood>> consumedFoodSetTypeToken = new TypeToken<>() {};
    private static final Gson gson = new Gson();

    private static final AttachmentType<Integer> CONSUMED_NUTRITION_ATTACHMENT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "consumed_nutrition"),
            builder -> builder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
                    .syncWith(ByteBufCodecs.INT, AttachmentSyncPredicate.targetOnly())
    );

    private static final AttachmentType<HashSet<ConsumedFood>> CONSUMED_FOODS_ATTACHMENT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "consumed_foods"),
            builder -> builder
                    .initializer(HashSet::new)
                    .persistent(Codec.STRING.xmap(string -> gson.fromJson(string, consumedFoodSetTypeToken), gson::toJson))
    );

    private final Player player;
    private final HashSet<ConsumedFood> consumedFoods;
    private int consumedNutrition;

    private HealthRegenerationComponent(Player player) {
        this.player = player;
        this.consumedNutrition = getConsumedNutrition(player);
        this.consumedFoods = player.getAttachedOrCreate(CONSUMED_FOODS_ATTACHMENT);
    }

    public static HealthRegenerationComponent get(Player player) {
        return new HealthRegenerationComponent(player);
    }

    private static void onServerTick(ServerLevel level) {
        if (!MelancholicConfig.disableHunger() || !MelancholicConfig.gradualHealthRegeneration()) {
            return;
        }
        for (var player : level.players()) {
            HealthRegenerationComponent.get(player).serverTick();
        }
    }

    private void serverTick() {
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
        player.setAttached(CONSUMED_NUTRITION_ATTACHMENT, consumedNutrition);
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

    public static int getConsumedNutrition(Player player) {
        if (!MelancholicConfig.gradualHealthRegeneration()) {
            return 0;
        }
        return player.getAttachedOrCreate(CONSUMED_NUTRITION_ATTACHMENT);
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(HealthRegenerationComponent::onServerTick);
    }
}
