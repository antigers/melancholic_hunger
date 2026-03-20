package antigers.melancholic_hunger.components;

import java.util.HashSet;
import java.util.function.Supplier;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

public class HealthRegenerationComponent implements ValueIOSerializable {

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

    private static class SyncHandler implements AttachmentSyncHandler<HealthRegenerationComponent> {
        @Override
        public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
            return holder == to; // only sync with the provider itself
        }

        @Override
        public void write(RegistryFriendlyByteBuf buf, HealthRegenerationComponent attachment, boolean initialSync) {
            buf.writeInt(attachment.consumedNutrition);
        }

        @Override
        public @Nullable HealthRegenerationComponent read(
				IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable HealthRegenerationComponent previousValue
        ) {
            HealthRegenerationComponent attachment = HealthRegenerationComponent.get((Player) holder);
            attachment.consumedNutrition = buf.readInt();
            return attachment;
        }
    }

    private static final Supplier<AttachmentType<HealthRegenerationComponent>> ATTACHMENT = Components.registerAttachment(
            "health_regeneration", () -> AttachmentType
                    .serializable(holder -> new HealthRegenerationComponent((Player) holder))
                    .sync(new SyncHandler())
                    .build()
    );

    private final TypeToken<HashSet<ConsumedFood>> consumedFoodSetTypeToken = new TypeToken<>() {};
    private static final Gson gson = new Gson();

    private final Player player;
    private HashSet<ConsumedFood> consumedFoods = new HashSet<>();
    private int consumedNutrition = 0;

    private HealthRegenerationComponent(Player player) {
        this.player = player;
    }

    public static HealthRegenerationComponent get(Player player) {
        return player.getData(ATTACHMENT);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("consumedNutrition", consumedNutrition);
        output.putString("consumedFoods", gson.toJson(consumedFoods));
    }

    @Override
    public void deserialize(ValueInput input) {
        consumedNutrition = input.getIntOr("consumedNutrition", 0);
        input.getString("consumedFoods").ifPresent(
                consumedFoodsStr -> consumedFoods = gson.fromJson(consumedFoodsStr, consumedFoodSetTypeToken)
        );
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Level level = event.getEntity().level();
        if (level instanceof ServerLevel serverLevel) {
            onServerTick(serverLevel);
        }
    }

    private static void onServerTick(ServerLevel level) {
        if (!MelancholicConfig.gradualHealthRegeneration()) {
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
        player.syncData(ATTACHMENT);
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
        return HealthRegenerationComponent.get(player).consumedNutrition;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HealthRegenerationComponent::onPlayerTick);
    }
}
