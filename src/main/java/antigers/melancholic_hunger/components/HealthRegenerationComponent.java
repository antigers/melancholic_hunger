package antigers.melancholic_hunger.components;

import java.util.HashSet;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.YACLConfig;
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
        if (!YACLConfig.gradualHealthRegeneration()) {
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
        if (YACLConfig.stopRegenerationAtFullHealth() && player.getHealth() >= player.getMaxHealth()) {
            consumedFoods.clear();
            consumedNutrition = 0;
            sync();
            return;
        }
        var digestingFoods = new HashSet<Integer>();
        boolean needsSync = false;
        float regenSpeedMultiplier = 1.0F;
        if (InstalledMods.FARMERS_DELIGHT && NourishmentEffectHandler.playerHasEffect(player)) {
            regenSpeedMultiplier = YACLConfig.nourishmentRegenSpeedMultiplier();
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
        if (!YACLConfig.disableHunger()) {
            return player.getFoodData().needsFood();
        }
        if (!YACLConfig.gradualHealthRegeneration()) {
            return player.getHealth() < player.getMaxHealth();
        }
        return player.getHealth() + consumedNutrition < player.getMaxHealth();
    }

    public void eat(ItemStack itemStack, FoodProperties foodProperties) {
        if (!(player instanceof ServerPlayer) || !YACLConfig.disableHunger()) {
            return;
        }
        var foodHealth = YACLConfig.getFoodHealth(itemStack, foodProperties);
        if (YACLConfig.gradualHealthRegeneration()) {
            consumedNutrition += foodHealth;
            consumedFoods.add(new ConsumedFood(foodProperties, foodHealth));
            sync();
        }
        else {
            player.heal(foodHealth);
        }
    }

    public static int getConsumedNutrition(Player player) {
        if (!YACLConfig.gradualHealthRegeneration()) {
            return 0;
        }
        return player.getAttachedOrCreate(CONSUMED_NUTRITION_ATTACHMENT);
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(HealthRegenerationComponent::onServerTick);
    }
}
