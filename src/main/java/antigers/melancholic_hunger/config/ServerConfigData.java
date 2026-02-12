package antigers.melancholic_hunger.config;

import java.util.LinkedHashMap;

public class ServerConfigData {
    public Boolean disableHunger;
    public HungerEffectOption hungerEffect;
    public Boolean gradualHealthRegeneration;
    public Float gradualHealthRegenerationSpeed;
    public Boolean useCustomFoodStackSizes;
    public LinkedHashMap<String, Integer> customFoodStackSizes;
    public SprintingOption sprinting;
    public Integer sprintingHealthLimit;
    public Boolean instantEating;
    public Boolean showFoodItemTooltips;
    public Integer nourishmentHealthBoostHeartsCount;
    public Float nourishmentRegenSpeedMultiplier;

    public record ImmutableServerConfigData (
            Boolean disableHunger,
            HungerEffectOption hungerEffect,
            Boolean gradualHealthRegeneration,
            Float gradualHealthRegenerationSpeed,
            Boolean useCustomFoodStackSizes,
            LinkedHashMap<String, Integer> customFoodStackSizes,
            SprintingOption sprinting,
            Integer sprintingHealthLimit,
            Boolean instantEating,
            Boolean showFoodItemTooltips,
            Integer nourishmentHealthBoostHeartsCount,
            Float nourishmentRegenSpeedMultiplier
    ) {}

    public ImmutableServerConfigData getImmutable() {
        return new ImmutableServerConfigData(
                disableHunger, hungerEffect, gradualHealthRegeneration, gradualHealthRegenerationSpeed,
                useCustomFoodStackSizes, customFoodStackSizes, sprinting, sprintingHealthLimit, instantEating,
                showFoodItemTooltips, nourishmentHealthBoostHeartsCount, nourishmentRegenSpeedMultiplier
        );
    }
}
