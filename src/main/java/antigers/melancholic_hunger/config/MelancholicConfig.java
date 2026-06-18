package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.components.ServerConfigComponent;
import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import antigers.melancholic_hunger.components.PlayerComponents;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import mod.adrenix.nostalgic.config.factory.ConfigBuilder;
import mod.adrenix.nostalgic.tweak.config.GameplayTweak;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vectorwing.farmersdelight.common.registry.ModItems;

import javax.lang.model.type.NullType;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MelancholicConfig {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";
    private static boolean isLoadedFromDisk = false;

    @SerialEntry(value = "clientOptions")
    private static ClientConfigData clientData = new ClientConfigData();
    @SerialEntry(value = "serverOptions")
    private static ServerConfigData serverData = new ServerConfigData();

    public static int getFoodHealth(ItemStack itemStack, FoodProperties foodProperties) {
        if (InstalledMods.NOSTALGIC_TWEAKS && GameplayTweak.CUSTOM_FOOD_HEALTH.get().containsItem(itemStack)) {
            return GameplayTweak.CUSTOM_FOOD_HEALTH.get().valueFrom(itemStack);
        }
        return foodProperties.nutrition();
    }

    private static final ConfigClassHandler<MelancholicConfig> HANDLER = ConfigClassHandler.createBuilder(MelancholicConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath(MelancholicHunger.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("melancholic_hunger.json5"))
                    .setJson5(true)
                    .build())
            .build();

    private static final ConfigOption<Boolean, NullType> DISABLE_HUNGER = new ConfigOption<>(
            "disableHunger", true, true, true,
            () -> serverData.disableHunger, val -> serverData.disableHunger = val
    );

    private static final ConfigOption<Boolean, Boolean> HIDE_HUNGER_BAR = new ConfigOption<Boolean, Boolean>(
            "hideHungerBar", true, true, false,
            () -> clientData.hideHungerBar, val -> clientData.hideHungerBar = val
    ).addDependency(DISABLE_HUNGER, false);

    private static final ConfigOption<HungerEffectOption, Boolean> HUNGER_EFFECT = new ConfigOption<HungerEffectOption, Boolean>(
            "hungerEffect", HungerEffectOption.REPLACED_WITH_OTHER, true, true,
            () -> serverData.hungerEffect, val -> serverData.hungerEffect = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<String, HungerEffectOption> HUNGER_REPLACEMENT_EFFECT = new ConfigOption<String, HungerEffectOption>(
            "hungerReplacementEffect", "minecraft:poison", false, true,
            () -> serverData.hungerReplacementEffect, val -> serverData.hungerReplacementEffect = val
    ).addDependency(HUNGER_EFFECT, HungerEffectOption.REPLACED_WITH_OTHER);

    private static final ConfigOption<Float, HungerEffectOption> HUNGER_REPLACEMENT_DURATION_MULTIPLIER = new ConfigOption<Float, HungerEffectOption>(
            "hungerReplacementDurationMultiplier", 0.5F, false, true,
            () -> serverData.hungerReplacementDurationMultiplier, val -> serverData.hungerReplacementDurationMultiplier = val
    ).addDependency(HUNGER_EFFECT, HungerEffectOption.REPLACED_WITH_OTHER);

    private static final ConfigOption<Boolean, Boolean> GRADUAL_HEALTH_REGENERATION = new ConfigOption<Boolean, Boolean>(
            "gradualHealthRegeneration", true, false, true,
            () -> serverData.gradualHealthRegeneration, val -> serverData.gradualHealthRegeneration = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<Float, Boolean> GRADUAL_HEALTH_REGENERATION_SPEED = new ConfigOption<Float, Boolean>(
            "gradualHealthRegenerationSpeed", 1.0F, false, true,
            () -> serverData.gradualHealthRegenerationSpeed, val -> serverData.gradualHealthRegenerationSpeed = val
    ).addDependency(GRADUAL_HEALTH_REGENERATION, true);

    private static final ConfigOption<Boolean, Boolean> SATURATION_BASED_REGENERATION = new ConfigOption<Boolean, Boolean>(
            "saturationBasedRegeneration", true, false, true,
            () -> serverData.saturationBasedRegeneration, val -> serverData.saturationBasedRegeneration = val
    ).addDependency(GRADUAL_HEALTH_REGENERATION, true);

    private static final ConfigOption<RegenerationAtFullHealthOption, Boolean> REGENERATION_AT_FULL_HEALTH = (
            new ConfigOption<RegenerationAtFullHealthOption, Boolean>(
                    "regenerationAtFullHealth", RegenerationAtFullHealthOption.CONTINUED, false, true,
                    () -> serverData.regenerationAtFullHealth, val -> serverData.regenerationAtFullHealth = val
            ).addDependency(GRADUAL_HEALTH_REGENERATION, true)
    );

    private static final ConfigOption<Boolean, Boolean> INSTANT_EATING = new ConfigOption<>(
            "instantEating", false, true, true,
            () -> serverData.instantEating, val -> serverData.instantEating = val
    );

    private static final ConfigOption<Boolean, Boolean> HIGHLIGHT_RESTORED_HEARTS = new ConfigOption<Boolean, Boolean>(
            "highlightRestoredHearts", true, false, false,
            () -> clientData.highlightRestoredHearts, val -> clientData.highlightRestoredHearts = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<HeartTextureOption, Boolean> RESTORED_HEARTS_TEXTURE = new ConfigOption<HeartTextureOption, Boolean>(
            "restoredHeartsTexture", HeartTextureOption.BLINKING, false, false,
            () -> clientData.restoredHeartsTexture, val -> clientData.restoredHeartsTexture = val
    ).addDependency(HIGHLIGHT_RESTORED_HEARTS, true);

    private static final ConfigOption<Color, Boolean> RESTORED_HEARTS_OVERLAY_COLOR = new ConfigOption<Color, Boolean>(
            "restoredHeartsOverlayColor", new Color(120, 0, 20), false, false,
            () -> clientData.restoredHeartsOverlayColor, val -> clientData.restoredHeartsOverlayColor = val
    ).addDependency(HIGHLIGHT_RESTORED_HEARTS, true);

    private static final ConfigOption<Boolean, Boolean> HIGHLIGHT_REGENERATED_HEARTS = new ConfigOption<Boolean, Boolean>(
            "highlightRegeneratedHearts", true, false, false,
            () -> clientData.highlightRegeneratedHearts, val -> clientData.highlightRegeneratedHearts = val
    ).addDependency(GRADUAL_HEALTH_REGENERATION, true);

    private static final ConfigOption<HeartTextureOption, Boolean> REGENERATED_HEARTS_TEXTURE = new ConfigOption<HeartTextureOption, Boolean>(
            "regeneratedHeartsTexture", HeartTextureOption.BLINKING, false, false,
            () -> clientData.regeneratedHeartsTexture, val -> clientData.regeneratedHeartsTexture = val
    ).addDependency(HIGHLIGHT_REGENERATED_HEARTS, true);

    private static final ConfigOption<Color, Boolean> REGENERATED_HEARTS_OVERLAY_COLOR = new ConfigOption<Color, Boolean>(
            "regeneratedHeartsOverlayColor", new Color(255, 135, 135), false, false,
            () -> clientData.regeneratedHeartsOverlayColor, val -> clientData.regeneratedHeartsOverlayColor = val
    ).addDependency(HIGHLIGHT_REGENERATED_HEARTS, true);

    private static final ConfigOption<Float, Boolean> REGENERATED_HEARTS_OPACITY_MIN = new ConfigOption<Float, Boolean>(
            "regeneratedHeartsOpacityMin", 0.1F, false, false,
            () -> clientData.regeneratedHeartsOpacityMin, val -> clientData.regeneratedHeartsOpacityMin = val
    ).addDependency(HIGHLIGHT_REGENERATED_HEARTS, true);

    private static final ConfigOption<Float, Boolean> REGENERATED_HEARTS_OPACITY_MAX = new ConfigOption<Float, Boolean>(
            "regeneratedHeartsOpacityMax", 1F, false, false,
            () -> clientData.regeneratedHeartsOpacityMax, val -> clientData.regeneratedHeartsOpacityMax = val
    ).addDependency(HIGHLIGHT_REGENERATED_HEARTS, true);

    private static final ConfigOption<Integer, Boolean> REGENERATED_HEARTS_BLINK_PERIOD = new ConfigOption<Integer, Boolean>(
            "regeneratedHeartsBlinkingPeriod", 1500, false, false,
            () -> clientData.regeneratedHeartsBlinkingPeriod, val -> clientData.regeneratedHeartsBlinkingPeriod = val
    ).addDependency(HIGHLIGHT_REGENERATED_HEARTS, true);

    private static final ConfigOption<Boolean, Boolean> SHOW_FOOD_ITEM_TOOLTIPS = new ConfigOption<Boolean, Boolean>(
            "showFoodItemTooltips", true, false, true,
            () -> serverData.showFoodItemTooltips, val -> serverData.showFoodItemTooltips = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<SprintingOption, NullType> SPRINTING = new ConfigOption<>(
            "sprinting", SprintingOption.LIMITED_BY_HEALTH, true, true,
            () -> serverData.sprinting, val -> serverData.sprinting = val
    );

    private static final ConfigOption<Integer, SprintingOption> SPRINTING_HEALTH_LIMIT = (
            new ConfigOption<Integer, SprintingOption>(
                    "sprintingHealthLimit", 6, false, true,
                    () -> serverData.sprintingHealthLimit, val -> serverData.sprintingHealthLimit = val
            ).addDependency(SPRINTING, SprintingOption.LIMITED_BY_HEALTH)
    );

    private static final ConfigOption<Boolean, Boolean> USE_CUSTOM_FOOD_STACK_SIZES = new ConfigOption<>(
            "useCustomFoodStackSizes", true, true, true,
            () -> serverData.useCustomFoodStackSizes, val -> serverData.useCustomFoodStackSizes = val
    );

    private static LinkedHashMap<String, Integer> getDefaultItemStackSizes() {
        var sizes = new LinkedHashMap<Item, Integer>();
        // nutrition = 1. Total 64
        sizes.put(Items.BEETROOT, 64);
        sizes.put(Items.DRIED_KELP, 64);

        // nutrition = 2. Total 64
        sizes.put(Items.COOKIE, 32);
        sizes.put(Items.GLOW_BERRIES, 32);
        sizes.put(Items.MELON_SLICE, 32);
        sizes.put(Items.SWEET_BERRIES, 32);

        // 3 <= nutrition <= 4. Total 48 - 64
        sizes.put(Items.APPLE, 16);
        sizes.put(Items.ENCHANTED_GOLDEN_APPLE, 16);
        sizes.put(Items.GOLDEN_APPLE, 16);
        sizes.put(Items.POTATO, 16);
        sizes.put(Items.BAKED_POTATO, 16);
        sizes.put(Items.POISONOUS_POTATO, 16);
        sizes.put(Items.CARROT, 16);
        sizes.put(Items.GOLDEN_CARROT, 16);
        sizes.put(Items.CHORUS_FRUIT, 16);

        // 5 <= nutrition <= 6. Total 40 - 48
        sizes.put(Items.BREAD, 8);
        sizes.put(Items.BEETROOT_SOUP, 8);
        sizes.put(Items.COD, 8);
        sizes.put(Items.COOKED_COD, 8);
        sizes.put(Items.SALMON, 8);
        sizes.put(Items.COOKED_SALMON, 8);
        sizes.put(Items.PUFFERFISH, 8);
        sizes.put(Items.TROPICAL_FISH, 8);
        sizes.put(Items.CHICKEN, 8);
        sizes.put(Items.COOKED_CHICKEN, 8);
        sizes.put(Items.MUTTON, 8);
        sizes.put(Items.COOKED_MUTTON, 8);
        sizes.put(Items.RABBIT, 8);
        sizes.put(Items.COOKED_RABBIT, 8);
        sizes.put(Items.HONEY_BOTTLE, 8);
        sizes.put(Items.MUSHROOM_STEW, 8);

        // nutrition = 8. Total 36
        sizes.put(Items.BEEF, 6);
        sizes.put(Items.COOKED_BEEF, 6);
        sizes.put(Items.PORKCHOP, 6);
        sizes.put(Items.COOKED_PORKCHOP, 6);
        sizes.put(Items.PUMPKIN_PIE, 6);

        // nutrition = 10. Total 40
        sizes.put(Items.RABBIT_STEW, 4);

        // unstackable
        sizes.put(Items.SUSPICIOUS_STEW, 1);

        var result = new LinkedHashMap<String, Integer>();
        for (var entry : sizes.entrySet()) {
            result.put(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getValue());
        }
        return result;
    }

    private static final ItemIntegerMapConfigOption CUSTOM_FOOD_STACK_SIZES = (ItemIntegerMapConfigOption) new ItemIntegerMapConfigOption(
            "customFoodStackSizes", getDefaultItemStackSizes(), true, true,
            () -> serverData.customFoodStackSizes, val -> serverData.customFoodStackSizes = val
    ).addDependency(USE_CUSTOM_FOOD_STACK_SIZES, true);

    private static LinkedHashMap<String, Integer> getFarmersDelightDefaultItemStackSizes() {
        if (!InstalledMods.FARMERS_DELIGHT) {
            return null;
        }
        var sizes = new LinkedHashMap<Supplier<Item>, Integer>();
        // nutrition = 1. Total 64
        sizes.put(ModItems.CABBAGE_LEAF, 64);
        sizes.put(ModItems.TOMATO, 64);

        // nutrition = 2. Total 64
        sizes.put(ModItems.CABBAGE, 32);
        sizes.put(ModItems.HONEY_COOKIE, 32);
        sizes.put(ModItems.ONION, 32);
        sizes.put(ModItems.PIE_CRUST, 32);
        sizes.put(ModItems.PUMPKIN_SLICE, 32);
        sizes.put(ModItems.RAW_PASTA, 32);
        sizes.put(ModItems.SWEET_BERRY_COOKIE, 32);
        sizes.put(ModItems.WHEAT_DOUGH, 32);

        // nutrition = 3. Total 72
        sizes.put(ModItems.APPLE_PIE_SLICE, 24);
        sizes.put(ModItems.CAKE_SLICE, 24);
        sizes.put(ModItems.CHOCOLATE_PIE_SLICE, 24);
        sizes.put(ModItems.SWEET_BERRY_CHEESECAKE_SLICE, 24);
        sizes.put(ModItems.MELON_POPSICLE, 24);
        sizes.put(ModItems.CHICKEN_CUTS, 24);
        sizes.put(ModItems.COOKED_CHICKEN_CUTS, 24);
        sizes.put(ModItems.COD_SLICE, 24);
        sizes.put(ModItems.COOKED_COD_SLICE, 24);
        sizes.put(ModItems.MUTTON_CHOPS, 24);
        sizes.put(ModItems.COOKED_MUTTON_CHOPS, 24);
        sizes.put(ModItems.SALMON_SLICE, 24);
        sizes.put(ModItems.COOKED_SALMON_SLICE, 24);

        // 4 <= nutrition <= 5. Total 64 - 80
        sizes.put(ModItems.MINCED_BEEF, 16);
        sizes.put(ModItems.BEEF_PATTY, 16);
        sizes.put(ModItems.BACON, 16);
        sizes.put(ModItems.COOKED_BACON, 16);
        sizes.put(ModItems.DOG_FOOD, 16);
        sizes.put(ModItems.FRIED_EGG, 16);
        sizes.put(ModItems.TOMATO_SAUCE, 16);
        sizes.put(ModItems.CABBAGE_ROLLS, 16);
        sizes.put(ModItems.NETHER_SALAD, 16);

        // 6 <= nutrition <= 7. Total 72 - 84
        sizes.put(ModItems.COOKED_RICE, 12);
        sizes.put(ModItems.FRUIT_SALAD, 12);
        sizes.put(ModItems.KELP_ROLL_SLICE, 12);
        sizes.put(ModItems.MIXED_SALAD, 12);
        sizes.put(ModItems.COD_ROLL, 12);
        sizes.put(ModItems.GLOW_BERRY_CUSTARD, 12);
        sizes.put(ModItems.SALMON_ROLL, 12);

        // 8 <= nutrition <= 10. Total 64 - 80
        sizes.put(ModItems.BARBECUE_STICK, 8);
        sizes.put(ModItems.BONE_BROTH, 8);
        sizes.put(ModItems.DUMPLINGS, 8);
        sizes.put(ModItems.EGG_SANDWICH, 8);
        sizes.put(ModItems.BACON_AND_EGGS, 8);
        sizes.put(ModItems.BACON_SANDWICH, 8);
        sizes.put(ModItems.CHICKEN_SANDWICH, 8);
        sizes.put(ModItems.MUTTON_WRAP, 8);
        sizes.put(ModItems.RATATOUILLE, 8);
        sizes.put(ModItems.HAM, 8);
        sizes.put(ModItems.SMOKED_HAM, 8);
        sizes.put(ModItems.STUFFED_POTATO, 8);

        // 11 <= nutrition <= 12. Total 66 - 72
        sizes.put(ModItems.APPLE_PIE, 6);
        sizes.put(ModItems.BEEF_STEW, 6);
        sizes.put(ModItems.CHOCOLATE_PIE, 6);
        sizes.put(ModItems.FISH_STEW, 6);
        sizes.put(ModItems.HAMBURGER, 6);
        sizes.put(ModItems.KELP_ROLL, 6);
        sizes.put(ModItems.MUSHROOM_RICE, 6);
        sizes.put(ModItems.PASTA_WITH_MEATBALLS, 6);
        sizes.put(ModItems.PASTA_WITH_MUTTON_CHOP, 6);
        sizes.put(ModItems.STEAK_AND_POTATOES, 6);
        sizes.put(ModItems.SWEET_BERRY_CHEESECAKE, 6);
        sizes.put(ModItems.VEGETABLE_SOUP, 6);

        // nutrition >= 14. Total 56
        sizes.put(ModItems.BAKED_COD_STEW, 4);
        sizes.put(ModItems.CHICKEN_SOUP, 4);
        sizes.put(ModItems.FRIED_RICE, 4);
        sizes.put(ModItems.GRILLED_SALMON, 4);
        sizes.put(ModItems.HONEY_GLAZED_HAM, 4);
        sizes.put(ModItems.NOODLE_SOUP, 4);
        sizes.put(ModItems.PUMPKIN_SOUP, 4);
        sizes.put(ModItems.ROASTED_MUTTON_CHOPS, 4);
        sizes.put(ModItems.ROAST_CHICKEN, 4);
        sizes.put(ModItems.SHEPHERDS_PIE, 4);
        sizes.put(ModItems.SQUID_INK_PASTA, 4);
        sizes.put(ModItems.STUFFED_PUMPKIN, 4);
        sizes.put(ModItems.VEGETABLE_NOODLES, 4);

        var result = new LinkedHashMap<String, Integer>();
        for (var entry : sizes.entrySet()) {
            result.put(BuiltInRegistries.ITEM.getKey(entry.getKey().get()).toString(), entry.getValue());
        }
        return result;
    }

    private static final ItemIntegerMapConfigOption FARMERS_DELIGHT_FOOD_STACK_SIZES = (ItemIntegerMapConfigOption) new ItemIntegerMapConfigOption(
            "farmersDelightFoodStackSizes", getFarmersDelightDefaultItemStackSizes(), false, true,
            () -> serverData.farmersDelightFoodStackSizes, val -> serverData.farmersDelightFoodStackSizes = val
    ).addDependency(USE_CUSTOM_FOOD_STACK_SIZES, true);

    private static final ConfigOption<Boolean, NullType> HIDE_EXPERIENCE_BAR = new ConfigOption<>(
            "hideExperienceBar", true, true, false,
            () -> clientData.hideExperienceBar, val -> clientData.hideExperienceBar = val
    );

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_IN_INVENTORY = (
            new ConfigOption<Boolean, Boolean>(
                    "showExperienceInInventory", true, false, false,
                    () -> clientData.showExperienceInInventory, val -> clientData.showExperienceInInventory = val
            ).addDependency(HIDE_EXPERIENCE_BAR, true)
    );

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_ON_SCREENS = new ConfigOption<Boolean, Boolean>(
            "showExperienceOnScreens", true, false, false,
            () -> clientData.showExperienceOnScreens, val -> clientData.showExperienceOnScreens = val
    ).addDependency(HIDE_EXPERIENCE_BAR, true);

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_ON_GAIN = new ConfigOption<Boolean, Boolean>(
            "showExperienceOnGain", true, false, false,
            () -> clientData.showExperienceOnGain, val -> clientData.showExperienceOnGain = val
    ).addDependency(HIDE_EXPERIENCE_BAR, true);

    private static final ConfigOption<Boolean, Boolean> ENABLE_EXPERIENCE_ANIMATION = new ConfigOption<Boolean, Boolean>(
            "enableExperienceAnimation", true, false, false,
            () -> clientData.enableExperienceAnimation, val -> clientData.enableExperienceAnimation = val
    ).addDependency(HIDE_EXPERIENCE_BAR, true);

    private static final ConfigOption<Integer, Boolean> EXPERIENCE_ANIMATION_DURATION = new ConfigOption<Integer, Boolean>(
            "experienceAnimationDuration", 150, false, false,
            () -> clientData.experienceAnimationDuration, val -> clientData.experienceAnimationDuration = val
    ).addDependency(ENABLE_EXPERIENCE_ANIMATION, true);

    private static final ConfigOption<Boolean, Boolean> RENDER_EXPERIENCE_OVER_BACKGROUND = new ConfigOption<Boolean, Boolean>(
            "renderExperienceOverBackground", true, false, false,
            () -> clientData.renderExperienceOverBackground, val -> clientData.renderExperienceOverBackground = val
    ).addDependency(HIDE_EXPERIENCE_BAR, true);

    private static final ConfigOption<Integer, Boolean> NOURISHMENT_HEALTH_BOOST_COUNT = new ConfigOption<Integer, Boolean>(
            "nourishmentHealthBoostHeartsCount", 3, false, true,
            () -> serverData.nourishmentHealthBoostHeartsCount, val -> serverData.nourishmentHealthBoostHeartsCount = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<Float, Boolean> NOURISHMENT_REGEN_SPEED_MULTIPLIER = new ConfigOption<Float, Boolean>(
            "nourishmentRegenSpeedMultiplier", 1.5F, false, true,
            () -> serverData.nourishmentRegenSpeedMultiplier, val -> serverData.nourishmentRegenSpeedMultiplier = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final List<ConfigOption<?, ?>> ALL_OPTIONS = List.of(
            DISABLE_HUNGER, GRADUAL_HEALTH_REGENERATION, GRADUAL_HEALTH_REGENERATION_SPEED, SATURATION_BASED_REGENERATION, REGENERATION_AT_FULL_HEALTH, HIDE_HUNGER_BAR,
            HUNGER_EFFECT, HUNGER_REPLACEMENT_EFFECT, HUNGER_REPLACEMENT_DURATION_MULTIPLIER, HIGHLIGHT_REGENERATED_HEARTS, REGENERATED_HEARTS_TEXTURE,
            REGENERATED_HEARTS_OVERLAY_COLOR, REGENERATED_HEARTS_OPACITY_MIN, REGENERATED_HEARTS_OPACITY_MAX, REGENERATED_HEARTS_BLINK_PERIOD, INSTANT_EATING,
            SHOW_FOOD_ITEM_TOOLTIPS, USE_CUSTOM_FOOD_STACK_SIZES, CUSTOM_FOOD_STACK_SIZES, FARMERS_DELIGHT_FOOD_STACK_SIZES, SPRINTING, SPRINTING_HEALTH_LIMIT,
            HIGHLIGHT_RESTORED_HEARTS, RESTORED_HEARTS_TEXTURE, RESTORED_HEARTS_OVERLAY_COLOR, HIDE_EXPERIENCE_BAR, SHOW_EXPERIENCE_IN_INVENTORY, SHOW_EXPERIENCE_ON_SCREENS,
            SHOW_EXPERIENCE_ON_GAIN, ENABLE_EXPERIENCE_ANIMATION, EXPERIENCE_ANIMATION_DURATION, RENDER_EXPERIENCE_OVER_BACKGROUND, NOURISHMENT_HEALTH_BOOST_COUNT,
            NOURISHMENT_REGEN_SPEED_MULTIPLIER
    );

    private static BooleanControllerBuilder createBooleanController(Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).yesNoFormatter().coloured(true);
    }

    private static FloatSliderControllerBuilder createTwoDigitsFloatController(Option<Float> option) {
        return FloatSliderControllerBuilder.create(option).range(0.0F, 1.0F).step(0.01F).formatValue(
                value -> Component.literal(String.format("%,.2f", value).replaceAll("[\u00a0\u202F]", " "))
        );
    }

    private static EnumControllerBuilder<HeartTextureOption> createHeartTextureController(Option<HeartTextureOption> option) {
        return EnumControllerBuilder.create(option).enumClass(HeartTextureOption.class)
                .formatValue(
                        value -> switch (value) {
                            case SINGLE_COLOR ->
                                    Component.translatable(CONFIG_PREFIX + "restoredHeartsTexture_single_color_option");
                            case ORIGINAL ->
                                    Component.translatable(CONFIG_PREFIX + "restoredHeartsTexture_original_option");
                            case BLINKING ->
                                    Component.translatable(CONFIG_PREFIX + "restoredHeartsTexture_blinking_option");
                        }
                );
    }

    private static ConfigCategory buildGameMechanicsCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "game_mechanics_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "game_mechanics_category_tooltip"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hunger_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hunger_group_description")))
                        .option(DISABLE_HUNGER.buildYACLOption(MelancholicConfig::createBooleanController))
                        .optionIf(InstalledMods.NOSTALGIC_TWEAKS, HIDE_HUNGER_BAR.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(HUNGER_EFFECT.buildYACLOption(
                                option -> EnumControllerBuilder.create(option).enumClass(HungerEffectOption.class)
                                        .formatValue(
                                                value -> switch (value) {
                                                    case VANILLA ->
                                                            Component.translatable(CONFIG_PREFIX + "hungerEffect_vanilla_option")
                                                                    // green
                                                                    .setStyle(Style.EMPTY.withColor(5635925));
                                                    case DISABLED ->
                                                            Component.translatable(CONFIG_PREFIX + "hungerEffect_disabled_option")
                                                                    // red
                                                                    .setStyle(Style.EMPTY.withColor(16733525));
                                                    case REPLACED_WITH_OTHER ->
                                                            Component.translatable(CONFIG_PREFIX + "hungerEffect_replaced_with_other_option")
                                                                    // yellow
                                                                    .setStyle(Style.EMPTY.withColor(16777045));
                                                }
                                        )
                        ))
                        .option(HUNGER_REPLACEMENT_EFFECT.buildYACLOption(StringControllerBuilder::create))
                        .option(HUNGER_REPLACEMENT_DURATION_MULTIPLIER.buildYACLOption(
                                option -> FloatSliderControllerBuilder.create(option).range(0.1F, 10.0F).step(0.1F)
                        ))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "health_regeneration_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "health_regeneration_group_description")))
                        .option(GRADUAL_HEALTH_REGENERATION.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(GRADUAL_HEALTH_REGENERATION_SPEED.buildYACLOption(
                                option -> FloatSliderControllerBuilder.create(option).range(0.1F, 10.0F).step(0.1F)
                        ))
                        .option(SATURATION_BASED_REGENERATION.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(REGENERATION_AT_FULL_HEALTH.buildYACLOption(
                                option -> EnumControllerBuilder.create(option).enumClass(RegenerationAtFullHealthOption.class)
                                        .formatValue(
                                                value -> switch (value) {
                                                    case STOPPED ->
                                                            Component.translatable(CONFIG_PREFIX + "regenerationAtFullHealth_stopped_option")
                                                                    // red
                                                                    .setStyle(Style.EMPTY.withColor(16733525));
                                                    case CONTINUED ->
                                                            Component.translatable(CONFIG_PREFIX + "regenerationAtFullHealth_continued_option")
                                                                    // yellow
                                                                    .setStyle(Style.EMPTY.withColor(16777045));
                                                    case STORED ->
                                                            Component.translatable(CONFIG_PREFIX + "regenerationAtFullHealth_stored_option")
                                                                    // green
                                                                    .setStyle(Style.EMPTY.withColor(5635925));
                                                }
                                        )
                        ))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "eating_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "eating_group_description")))
                        .option(INSTANT_EATING.buildYACLOption(MelancholicConfig::createBooleanController))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "sprinting_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "sprinting_group_description")))
                        .option(SPRINTING.buildYACLOption(
                                option -> EnumControllerBuilder.create(option).enumClass(SprintingOption.class)
                                        .formatValue(
                                                value -> switch (value) {
                                                    case VANILLA ->
                                                            Component.translatable(CONFIG_PREFIX + "sprinting_vanilla_option")
                                                                    // green
                                                                    .setStyle(Style.EMPTY.withColor(5635925));
                                                    case DISABLED ->
                                                            Component.translatable(CONFIG_PREFIX + "sprinting_disabled_option")
                                                                    // red
                                                                    .setStyle(Style.EMPTY.withColor(16733525));
                                                    case LIMITED_BY_HEALTH ->
                                                            Component.translatable(CONFIG_PREFIX + "sprinting_limited_by_health_option")
                                                                    // yellow
                                                                    .setStyle(Style.EMPTY.withColor(16777045));
                                                }
                                        )
                        ))
                        .option(SPRINTING_HEALTH_LIMIT.buildYACLOption(
                                option -> IntegerSliderControllerBuilder.create(option).range(1, 20).step(1)
                        ))
                        .build())

                .build();
    }

    private static ConfigCategory buildHUDCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "hud_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "hud_category_tooltip"))
                .groupIf(InstalledMods.NOSTALGIC_TWEAKS, OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hud_hunger_bar_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hud_hunger_bar_group_description")))
                        .option(HIDE_HUNGER_BAR.buildYACLOption(MelancholicConfig::createBooleanController))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hud_restored_health_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hud_restored_health_group_description")))
                        .option(HIGHLIGHT_RESTORED_HEARTS.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(RESTORED_HEARTS_TEXTURE.buildYACLOption(MelancholicConfig::createHeartTextureController))
                        .option(RESTORED_HEARTS_OVERLAY_COLOR.buildYACLOption(option -> ColorControllerBuilder.create(option).allowAlpha(false)))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hud_regenerated_health_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hud_regenerated_health_group_description")))
                        .option(HIGHLIGHT_REGENERATED_HEARTS.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(REGENERATED_HEARTS_TEXTURE.buildYACLOption(MelancholicConfig::createHeartTextureController))
                        .option(REGENERATED_HEARTS_OVERLAY_COLOR.buildYACLOption(option -> ColorControllerBuilder.create(option).allowAlpha(false)))
                        .option(REGENERATED_HEARTS_OPACITY_MIN.buildYACLOption(MelancholicConfig::createTwoDigitsFloatController))
                        .option(REGENERATED_HEARTS_OPACITY_MAX.buildYACLOption(MelancholicConfig::createTwoDigitsFloatController))
                        .option(REGENERATED_HEARTS_BLINK_PERIOD.buildYACLOption(
                                option -> IntegerSliderControllerBuilder.create(option).range(500, 5000).step(100)
                        ))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hud_tooltips_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hud_tooltips_group_description")))
                        .option(SHOW_FOOD_ITEM_TOOLTIPS.buildYACLOption(MelancholicConfig::createBooleanController))
                        .build())

                .group(OptionGroup.createBuilder()
                        .name(Component.translatable(CONFIG_PREFIX + "hud_experience_group_name"))
                        .description(OptionDescription.of(Component.translatable(CONFIG_PREFIX + "hud_experience_group_description")))
                        .option(HIDE_EXPERIENCE_BAR.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(SHOW_EXPERIENCE_IN_INVENTORY.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(SHOW_EXPERIENCE_ON_SCREENS.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(SHOW_EXPERIENCE_ON_GAIN.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(RENDER_EXPERIENCE_OVER_BACKGROUND.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(ENABLE_EXPERIENCE_ANIMATION.buildYACLOption(MelancholicConfig::createBooleanController))
                        .option(EXPERIENCE_ANIMATION_DURATION.buildYACLOption(
                                option -> IntegerSliderControllerBuilder.create(option).range(10, 500).step(10)
                        ))
                        .build())
                .build();
    }

    private static void setAllFoodStacksTo1(YACLScreen screen, ButtonOption button) {
        LinkedHashMap<String, Integer> stacks = getDefaultItemStackSizes();
        stacks.replaceAll((k, v) -> 1);
        stacks.replace(Items.COOKIE.toString(), 8);
        stacks.replace(Items.BEETROOT.toString(), 8);
        stacks.replace(Items.CARROT.toString(), 8);
        stacks.replace(Items.CHORUS_FRUIT.toString(), 8);
        stacks.replace(Items.MELON_SLICE.toString(), 8);
        stacks.replace(Items.POTATO.toString(), 8);
        stacks.replace(Items.POISONOUS_POTATO.toString(), 8);
        stacks.replace(Items.SWEET_BERRIES.toString(), 8);
        stacks.replace(Items.GLOW_BERRIES.toString(), 8);
        stacks.replace(Items.DRIED_KELP.toString(), 9);
        stacks.replace(Items.HONEY_BOTTLE.toString(), 4);
        CUSTOM_FOOD_STACK_SIZES.updatePendingValue(stacks);

        if (!InstalledMods.FARMERS_DELIGHT) {
            return;
        }
        LinkedHashMap<String, Integer> farmersDelightStacks = getFarmersDelightDefaultItemStackSizes();
        farmersDelightStacks.replaceAll((k, v) -> 1);
        FARMERS_DELIGHT_FOOD_STACK_SIZES.updatePendingValue(farmersDelightStacks);
    }

    private static void setAllFoodStacksTo64(YACLScreen screen, ButtonOption button) {
        LinkedHashMap<String, Integer> stacks = getDefaultItemStackSizes();
        stacks.replaceAll((k, v) -> 64);
        stacks.replace(Items.SUSPICIOUS_STEW.toString(), 1);
        CUSTOM_FOOD_STACK_SIZES.updatePendingValue(stacks);

        if (!InstalledMods.FARMERS_DELIGHT) {
            return;
        }
        LinkedHashMap<String, Integer> farmersDelightStacks = getFarmersDelightDefaultItemStackSizes();
        farmersDelightStacks.replaceAll((k, v) -> 64);
        FARMERS_DELIGHT_FOOD_STACK_SIZES.updatePendingValue(farmersDelightStacks);
    }

    private static ButtonOption createButtonOption(
            String buttonName, BiConsumer<YACLScreen, ButtonOption> action, ConfigOption.ConfigOptionDependency<?> dependency
    ) {
        boolean playerHasPermission = ConfigOption.getPlayerHasPermission();
        var descriptionBuilder = OptionDescription.createBuilder();
        descriptionBuilder.text(Component.translatable(CONFIG_PREFIX + "button." + buttonName + ".description"));
        if (!playerHasPermission) {
            ConfigOption.addOpPrivilegesRequiredToDescription(descriptionBuilder);
        }
        ButtonOption buttonOption = ButtonOption.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "button." + buttonName + ".name"))
                .available(playerHasPermission)
                .description(descriptionBuilder.build()).text(Component.empty()).action(action).build();

        dependency.configOption().YACLOption.addEventListener(
                (option, event) -> {
                    if (event != OptionEventListener.Event.STATE_CHANGE && event != OptionEventListener.Event.AVAILABILITY_CHANGE) {
                        return;
                    }
                    buttonOption.setAvailable(dependency.isPendingValueEqualsRequired());
                }
        );
        return buttonOption;
    }

    private static ConfigCategory buildFoodItemsCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "food_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "food_category_tooltip"))
                .option(USE_CUSTOM_FOOD_STACK_SIZES.buildYACLOption(MelancholicConfig::createBooleanController))
                .option(CUSTOM_FOOD_STACK_SIZES.buildYACLOption())
                .option(createButtonOption(
                        "set_all_food_stack_sizes_to_1", MelancholicConfig::setAllFoodStacksTo1,
                        new ConfigOption.ConfigOptionDependency<>(USE_CUSTOM_FOOD_STACK_SIZES, true)
                ))
                .option(createButtonOption(
                        "set_all_food_stack_sizes_to_64", MelancholicConfig::setAllFoodStacksTo64,
                        new ConfigOption.ConfigOptionDependency<>(USE_CUSTOM_FOOD_STACK_SIZES, true)
                ))
                .build();
    }

    private static ConfigCategory buildFarmersDelightCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "farmers_delight_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "farmers_delight_category_tooltip"))
                .option(NOURISHMENT_HEALTH_BOOST_COUNT.buildYACLOption(
                        option -> IntegerSliderControllerBuilder.create(option).range(0, 10).step(1))
                )
                .option(NOURISHMENT_REGEN_SPEED_MULTIPLIER.buildYACLOption(
                        option -> FloatSliderControllerBuilder.create(option).range(1.0F, 5.0F).step(0.1F))
                )
                .option(FARMERS_DELIGHT_FOOD_STACK_SIZES.buildYACLOption())
                .build();
    }

    public static YetAnotherConfigLib getYACLInstance() {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Component.translatable(CONFIG_PREFIX + "title"))
                .category(buildGameMechanicsCategory())
                .category(buildHUDCategory())
                .category(buildFoodItemsCategory())
                .categoryIf(InstalledMods.FARMERS_DELIGHT, buildFarmersDelightCategory())
                .save(() -> {
                    var client = Minecraft.getInstance();
                    boolean isSinglePlayer = client.isSingleplayer();
                    boolean hasSingleplayerServer = client.hasSingleplayerServer();
                    var player = client.player;
                    if (!InstalledMods.NOSTALGIC_TWEAKS) {
                        // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
                        clientData.hideHungerBar = serverData.disableHunger;
                    }
                    if (isSinglePlayer || player == null || hasSingleplayerServer) {
                        // writing config file if in singleplayer or if on title screen
                        HANDLER.save();
                        if (hasSingleplayerServer) {
                            ServerConfigComponent.syncAllPlayersExceptOf(player.getId());
                        }
                    } else {
                        // sending config to the server if in multiplayer
                        PlayerComponents.SERVER_CONFIG.get(player).sendToServer(serverData.getImmutable());
                    }
                    // Syncing new settings to the nostalgic tweaks config.
                    // If in multiplayer, only the client config will be synced
                    if (InstalledMods.NOSTALGIC_TWEAKS) {
                        var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                        handler.melancholic_hunger$writeConfigToNT(
                                MelancholicConfig.serverData.getImmutable(), MelancholicConfig.clientData.getImmutable()
                        );
                        if (hasSingleplayerServer) {
                            ServerConfigComponent.syncNostalgicTweaksToAllPlayers();
                        }
                    }
                })
        );
    }

    private static void updateCurrentScreen() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        if (Minecraft.getInstance().screen instanceof YACLScreen) {
            ALL_OPTIONS.forEach(ConfigOption::forgetPendingValueIfServerOption);
        }
    }

    public static void loadFromDisk() {
        if (isLoadedFromDisk) {
            return;
        }
        HANDLER.load();
        for (var option : ALL_OPTIONS) {
            option.validateValue();
        }
        if (!InstalledMods.NOSTALGIC_TWEAKS) {
            // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
            HIDE_HUNGER_BAR.setValue(false);
        }
        isLoadedFromDisk = true;
    }

    public static void saveToDisk() {
        HANDLER.save();
    }

    public static ClientConfigData.ImmutableClientConfigData getClientData() {
        loadFromDisk();
        return clientData.getImmutable();
    }

    public static void setClientData(ClientConfigData.ImmutableClientConfigData newClientData) {
        HIDE_HUNGER_BAR.setValue(newClientData.hideHungerBar());
        HIGHLIGHT_REGENERATED_HEARTS.setValue(newClientData.highlightRegeneratedHearts());
        REGENERATED_HEARTS_TEXTURE.setValue(newClientData.regeneratedHeartsTexture());
        REGENERATED_HEARTS_OVERLAY_COLOR.setValue(newClientData.regeneratedHeartsOverlayColor());
        REGENERATED_HEARTS_OPACITY_MIN.setValue(newClientData.regeneratedHeartsOpacityMin());
        REGENERATED_HEARTS_OPACITY_MAX.setValue(newClientData.regeneratedHeartsOpacityMax());
        REGENERATED_HEARTS_BLINK_PERIOD.setValue(newClientData.regeneratedHeartsBlinkingPeriod());
        HIGHLIGHT_RESTORED_HEARTS.setValue(newClientData.highlightRestoredHearts());
        RESTORED_HEARTS_TEXTURE.setValue(newClientData.restoredHeartsTexture());
        RESTORED_HEARTS_OVERLAY_COLOR.setValue(newClientData.restoredHeartsOverlayColor());
        HIDE_EXPERIENCE_BAR.setValue(newClientData.hideExperienceBar());
        SHOW_EXPERIENCE_IN_INVENTORY.setValue(newClientData.showExperienceInInventory());
        SHOW_EXPERIENCE_ON_SCREENS.setValue(newClientData.showExperienceOnScreens());
        SHOW_EXPERIENCE_ON_GAIN.setValue(newClientData.showExperienceOnScreens());
        ENABLE_EXPERIENCE_ANIMATION.setValue(newClientData.enableExperienceAnimation());
        EXPERIENCE_ANIMATION_DURATION.setValue(newClientData.experienceAnimationDuration());
        RENDER_EXPERIENCE_OVER_BACKGROUND.setValue(newClientData.renderExperienceOverBackground());
    }

    public static ServerConfigData.ImmutableServerConfigData getServerData() {
        loadFromDisk();
        return serverData.getImmutable();
    }

    public static boolean setServerData(ServerConfigData.ImmutableServerConfigData newServerData) {
        if (serverData.getImmutable().equals(newServerData)) {
            return false;
        }
        DISABLE_HUNGER.setValue(newServerData.disableHunger());
        if (!InstalledMods.NOSTALGIC_TWEAKS) {
            // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
            HIDE_HUNGER_BAR.setValue(false);
        }
        HUNGER_EFFECT.setValue(newServerData.hungerEffect());
        HUNGER_REPLACEMENT_EFFECT.setValue(newServerData.hungerReplacementEffect());
        HUNGER_REPLACEMENT_DURATION_MULTIPLIER.setValue(newServerData.hungerReplacementDurationMultiplier());
        GRADUAL_HEALTH_REGENERATION.setValue(newServerData.gradualHealthRegeneration());
        GRADUAL_HEALTH_REGENERATION_SPEED.setValue(newServerData.gradualHealthRegenerationSpeed());
        SATURATION_BASED_REGENERATION.setValue(newServerData.saturationBasedRegeneration());
        REGENERATION_AT_FULL_HEALTH.setValue(newServerData.regenerationAtFullHealth());
        INSTANT_EATING.setValue(newServerData.instantEating());
        SHOW_FOOD_ITEM_TOOLTIPS.setValue(newServerData.showFoodItemTooltips());
        USE_CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.useCustomFoodStackSizes());
        CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.customFoodStackSizes());
        FARMERS_DELIGHT_FOOD_STACK_SIZES.setValue(newServerData.farmersDelightFoodStackSizes());
        SPRINTING.setValue(newServerData.sprinting());
        SPRINTING_HEALTH_LIMIT.setValue(newServerData.sprintingHealthLimit());
        NOURISHMENT_HEALTH_BOOST_COUNT.setValue(newServerData.nourishmentHealthBoostHeartsCount());
        NOURISHMENT_REGEN_SPEED_MULTIPLIER.setValue(newServerData.nourishmentRegenSpeedMultiplier());
        updateCurrentScreen();
        return true;
    }

    public static boolean disableHunger() {
        return serverData.disableHunger;
    }

    public static boolean hideHungerBar() {
        return clientData.hideHungerBar || disableHunger();
    }

    public static boolean highlightRegeneratedHearts() {
        return clientData.highlightRegeneratedHearts;
    }

    public static HeartTextureOption regeneratedHeartsTexture() {
        return clientData.regeneratedHeartsTexture;
    }

    public static Color regeneratedHeartsOverlayColor() {
        return clientData.regeneratedHeartsOverlayColor;
    }

    public static float regeneratedHeartsOpacityMin() {
        return clientData.regeneratedHeartsOpacityMin;
    }

    public static float regeneratedHeartsOpacityMax() {
        return clientData.regeneratedHeartsOpacityMax;
    }

    public static int regeneratedHeartsBlinkingPeriod() {
        return clientData.regeneratedHeartsBlinkingPeriod;
    }

    public static boolean highlightRestoredHearts() {
        return clientData.highlightRestoredHearts;
    }

    public static HeartTextureOption restoredHeartsTexture() {
        return clientData.restoredHeartsTexture;
    }

    public static Color restoredHeartsOverlayColor() {
        return clientData.restoredHeartsOverlayColor;
    }

    public static HungerEffectOption hungerEffect() {
        return serverData.hungerEffect;
    }

    public static Holder<MobEffect> hungerReplacementEffect() {
        Optional<Holder.Reference<MobEffect>> effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(
                ResourceLocation.parse(serverData.hungerReplacementEffect)
        );
        if (effectHolder.isEmpty()) {
            // using default poison effect if incorrect poison id is specified
            return MobEffects.POISON;
        }
        return effectHolder.get();
    }

    public static float hungerReplacementDurationMultiplier() {
        return serverData.hungerReplacementDurationMultiplier;
    }

    public static boolean gradualHealthRegeneration() {
        return serverData.gradualHealthRegeneration;
    }

    public static boolean saturationBasedRegeneration() {
        return serverData.saturationBasedRegeneration;
    }

    public static float gradualHealthRegenerationSpeed() {
        return serverData.gradualHealthRegenerationSpeed;
    }

    public static RegenerationAtFullHealthOption regenerationAtFullHealth() {
        return serverData.regenerationAtFullHealth;
    }

    public static boolean shouldInstantlyEat(Item item) {
        if (!serverData.instantEating) {
            return false;
        }
        if (InstalledMods.NOSTALGIC_TWEAKS) {
            return !GameplayTweak.IGNORED_EDIBLES.get().containsItem(item);
        }
        return true;
    }

    public static boolean showFoodItemTooltips() {
        return serverData.showFoodItemTooltips;
    }

    public static Integer getItemStackSize(ItemStack itemStack) {
        var itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        if (!serverData.useCustomFoodStackSizes) {
            return null;
        }
        if (serverData.customFoodStackSizes.containsKey(itemId)) {
            return serverData.customFoodStackSizes.get(itemId);
        }
        if (InstalledMods.FARMERS_DELIGHT && serverData.farmersDelightFoodStackSizes.containsKey(itemId)) {
            return serverData.farmersDelightFoodStackSizes.get(itemId);
        }
        return null;
    }

    public static SprintingOption sprinting() {
        return serverData.sprinting;
    }

    public static int sprintingHealthLimit() {
        return serverData.sprintingHealthLimit;
    }

    public static boolean hideExperienceBar() {
        return clientData.hideExperienceBar;
    }

    public static boolean showExperienceInInventory() {
        return clientData.showExperienceInInventory;
    }

    public static boolean showExperienceOnScreens() {
        return clientData.showExperienceOnScreens;
    }

    public static boolean showExperienceOnGain() {
        return clientData.showExperienceOnGain;
    }

    public static boolean enableExperienceAnimation() {
        return clientData.enableExperienceAnimation;
    }

    public static int experienceAnimationDuration() {
        return clientData.experienceAnimationDuration;
    }

    public static boolean renderExperienceOverBackground() {
        return clientData.renderExperienceOverBackground;
    }

    public static int nourishmentHealthBoostHeartsCount() {
        return serverData.nourishmentHealthBoostHeartsCount;
    }

    public static float nourishmentRegenSpeedMultiplier() {
        return serverData.nourishmentRegenSpeedMultiplier;
    }
}
