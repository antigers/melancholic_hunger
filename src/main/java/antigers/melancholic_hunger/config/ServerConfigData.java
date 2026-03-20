package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.MelancholicHunger;
import com.google.gson.Gson;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;

public class ServerConfigData {
    public Boolean disableHunger;
    public HungerEffectOption hungerEffect;
    public Boolean gradualHealthRegeneration;
    public Float gradualHealthRegenerationSpeed;
    public RegenerationAtFullHealthOption regenerationAtFullHealth;
    public Boolean useCustomFoodStackSizes;
    public LinkedHashMap<String, Integer> customFoodStackSizes;
    public LinkedHashMap<String, Integer> farmersDelightFoodStackSizes;
    public SprintingOption sprinting;
    public Integer sprintingHealthLimit;
    public Boolean instantEating;
    public Boolean showFoodItemTooltips;
    public Integer nourishmentHealthBoostHeartsCount;
    public Float nourishmentRegenSpeedMultiplier;

    public static final CustomPacketPayload.Type<ImmutableServerConfigData> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "server_config_component")
    );
    public static final Gson gson = new Gson();

    public record ImmutableServerConfigData (
            Boolean disableHunger,
            HungerEffectOption hungerEffect,
            Boolean gradualHealthRegeneration,
            Float gradualHealthRegenerationSpeed,
            RegenerationAtFullHealthOption regenerationAtFullHealth,
            Boolean useCustomFoodStackSizes,
            LinkedHashMap<String, Integer> customFoodStackSizes,
            LinkedHashMap<String, Integer> farmersDelightFoodStackSizes,
            SprintingOption sprinting,
            Integer sprintingHealthLimit,
            Boolean instantEating,
            Boolean showFoodItemTooltips,
            Integer nourishmentHealthBoostHeartsCount,
            Float nourishmentRegenSpeedMultiplier
    ) implements CustomPacketPayload {

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public String toJson() {
            return gson.toJson(this);
        }

        public static ImmutableServerConfigData fromJson(String json) {
            return gson.fromJson(json, ImmutableServerConfigData.class);
        }
    }

    public ImmutableServerConfigData getImmutable() {
        return new ImmutableServerConfigData(
                disableHunger, hungerEffect, gradualHealthRegeneration, gradualHealthRegenerationSpeed, regenerationAtFullHealth,
                useCustomFoodStackSizes, customFoodStackSizes, farmersDelightFoodStackSizes, sprinting, sprintingHealthLimit, instantEating,
                showFoodItemTooltips, nourishmentHealthBoostHeartsCount, nourishmentRegenSpeedMultiplier
        );
    }
}
