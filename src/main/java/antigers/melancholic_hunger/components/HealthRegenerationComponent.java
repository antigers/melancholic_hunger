package antigers.melancholic_hunger.components;

import java.util.HashSet;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.utils.ClientOnlyHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoRegisterCapability
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
                    1, (int)(10 / (foodComponent.getSaturationModifier() * YACLConfig.gradualHealthRegenerationSpeed()))
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

    private static class HashSetConsumedFood extends HashSet<ConsumedFood> {}

    private static class CapabilityProvider implements ICapabilitySerializable<CompoundTag> {

        private final Player player;
        private HealthRegenerationComponent component;
        private LazyOptional<HealthRegenerationComponent> optional = LazyOptional.of(this::getComponent);

        private CapabilityProvider(Player player) {
            this.player = player;
        }

        private HealthRegenerationComponent getComponent() {
            if (component == null) {
                component = new HealthRegenerationComponent(player);
            }
            return component;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, final @Nullable Direction side) {
            if (cap == CAPABILITY) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            HealthRegenerationComponent component = getComponent();
            nbt.putInt("consumedNutrition", component.consumedNutrition);
            nbt.putString("consumedFoods", gson.toJson(component.consumedFoods));
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            HealthRegenerationComponent component = getComponent();
            component.consumedNutrition = Math.max(nbt.getInt("consumedNutrition"), 0);
            var consumedFoodsStr = nbt.getString("consumedFoods");
            if (!consumedFoodsStr.isEmpty()) {
                component.consumedFoods = gson.fromJson(consumedFoodsStr, HashSetConsumedFood.class);
            }
        }
    }

    private static class SyncNetworkPacket {
        private int consumedNutrition;

        public SyncNetworkPacket(int consumedNutrition) {
            this.consumedNutrition = consumedNutrition;
        }

        public static SyncNetworkPacket decode(FriendlyByteBuf buf) {
            return new SyncNetworkPacket(buf.getInt(1));
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(consumedNutrition);
        }

        public void handle(NetworkEvent.Context context) {
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    HealthRegenerationComponent.get(ClientOnlyHelper.getPlayerOnClient()).consumedNutrition = this.consumedNutrition;
                }
            });
        }
    }

    private static final ResourceLocation CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "health_regeneration_component"
    );
    private static final Capability<HealthRegenerationComponent> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private static final Gson gson = new Gson();

    private final Player player;
    private HashSet<ConsumedFood> consumedFoods = new HashSet<>();
    private int consumedNutrition = 0;

    public HealthRegenerationComponent(Player player) {
        this.player = player;
    }

    public static HealthRegenerationComponent get(Player player) {
        return player.getCapability(CAPABILITY).orElseThrow(
                () -> new RuntimeException("Unable to get HealthRegenerationComponent capability for player " + player)
        );
    }

    private static void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        event.player.getCapability(CAPABILITY).ifPresent(HealthRegenerationComponent::serverTick);
    }

    private void serverTick() {
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
        if (player instanceof ServerPlayer serverPlayer) {
            Networking.sendToPlayer(serverPlayer, new SyncNetworkPacket(consumedNutrition));
        }
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
        if (!(player instanceof ServerPlayer) || !YACLConfig.disableHunger()) {
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

    public static int getConsumedNutrition(Player player) {
        if (!YACLConfig.gradualHealthRegeneration()) {
            return 0;
        }
		return player.getCapability(CAPABILITY).resolve()
                .map(component -> component.consumedNutrition)
                .orElse(0);
	}

    private static void attachCapabilityToPlayers(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(CAPABILITY).isPresent()) {
                event.addCapability(CAPABILITY_ID, new CapabilityProvider(player));
            }
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(HealthRegenerationComponent::onServerTick);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, HealthRegenerationComponent::attachCapabilityToPlayers);
        Networking.registerPacket(SyncNetworkPacket.class, SyncNetworkPacket::encode, SyncNetworkPacket::decode, SyncNetworkPacket::handle);
    }
}
