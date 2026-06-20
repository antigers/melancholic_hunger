package antigers.melancholic_hunger.config;

import java.util.LinkedHashMap;

public class ServerConfigData {
    public Boolean disableHunger;
    public HungerEffectOption hungerEffect;
    public String hungerReplacementEffect;
    public Float hungerReplacementDurationMultiplier;
    public Boolean gradualHealthRegeneration;
    public Float gradualHealthRegenerationSpeed;
    public Boolean saturationBasedRegeneration;
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

    public record ImmutableServerConfigData(
            Boolean disableHunger,
            HungerEffectOption hungerEffect,
            String hungerReplacementEffect,
            Float hungerReplacementDurationMultiplier,
            Boolean gradualHealthRegeneration,
            Float gradualHealthRegenerationSpeed,
            Boolean saturationBasedRegeneration,
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
    ) {
    }

    public ImmutableServerConfigData getImmutable() {
        return new ImmutableServerConfigData(
                disableHunger, hungerEffect, hungerReplacementEffect, hungerReplacementDurationMultiplier, gradualHealthRegeneration, gradualHealthRegenerationSpeed,
                saturationBasedRegeneration, regenerationAtFullHealth, useCustomFoodStackSizes, customFoodStackSizes, farmersDelightFoodStackSizes, sprinting,
                sprintingHealthLimit, instantEating, showFoodItemTooltips, nourishmentHealthBoostHeartsCount, nourishmentRegenSpeedMultiplier
        );
    }
}