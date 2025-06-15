package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.components.PlayerComponents;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.lang.model.type.NullType;
import java.util.*;

public class YACLConfig {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";

    @SerialEntry(value = "clientOptions")
    private static ClientConfigData clientData = new ClientConfigData();
    @SerialEntry(value = "serverOptions")
    private static ServerConfigData serverData = new ServerConfigData();

    private static HashMap<String, Integer> customFoodHealth = new HashMap<>();
    private static HashMap<String, Integer> customItemStackSizes = new HashMap<>();

    public static void setCustomFoodHealthMap(HashMap<String, Integer> foodHealthMap) {
        customFoodHealth = foodHealthMap;
    }

    public static void setCustomItemStackSizesMap(HashMap<String, Integer> itemStackSizesMap) {
        customItemStackSizes = itemStackSizesMap;
    }

    public static int getFoodHealth(ItemStack itemStack, FoodComponent foodComponent) {
        var itemId = Registries.ITEM.getId(itemStack.getItem()).toString();
        if (customFoodHealth.containsKey(itemId)) {
            return customFoodHealth.get(itemId);
        }
        return foodComponent.nutrition();
    }

    private static final ConfigClassHandler<YACLConfig> HANDLER = ConfigClassHandler.createBuilder(YACLConfig.class)
            .id(Identifier.of("melancholic_hunger", "config"))
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
    ).addValueDependency(DISABLE_HUNGER, false, false, true);

    private static final ConfigOption<HungerEffectOption, Boolean> HUNGER_EFFECT = (
            new ConfigOption<HungerEffectOption, Boolean>(
                    "hungerEffect", HungerEffectOption.REPLACED_WITH_POISON, true, true,
                    () -> serverData.hungerEffect, val -> serverData.hungerEffect = val
            ).addValueDependency(
                    DISABLE_HUNGER, true, HungerEffectOption.REPLACED_WITH_POISON, HungerEffectOption.VANILLA
            )
    );

    private static final ConfigOption<Boolean, Boolean> HIGHLIGHT_RESTORED_HEARTS = new ConfigOption<Boolean, Boolean>(
            "highlightRestoredHearts", true, false, false,
            () -> clientData.highlightRestoredHearts, val -> clientData.highlightRestoredHearts = val
    ).addValueDependency(DISABLE_HUNGER, true, true, false);

    private static final ConfigOption<Boolean, Boolean> GRADUAL_HEALTH_REGENERATION = (
            new ConfigOption<Boolean, Boolean>(
                    "gradualHealthRegeneration", true, false, true,
                    () -> serverData.gradualHealthRegeneration, val -> serverData.gradualHealthRegeneration = val
            ).addValueDependency(DISABLE_HUNGER, true, true, false)
    );

    private static final ConfigOption<Float, Boolean> GRADUAL_HEALTH_REGENERATION_SPEED = (
            new ConfigOption<Float, Boolean>(
                    "gradualHealthRegenerationSpeed", 1.0F, false, true,
                    () -> serverData.gradualHealthRegenerationSpeed, val -> serverData.gradualHealthRegenerationSpeed = val
            ).addDependency(GRADUAL_HEALTH_REGENERATION, true)
    );

    private static final ConfigOption<Boolean, Boolean> HIGHLIGHT_REGENERATED_HEARTS = (
            new ConfigOption<Boolean, Boolean>(
                    "highlightRegeneratedHearts",true, false, false,
                    () -> clientData.highlightRegeneratedHearts, val -> clientData.highlightRegeneratedHearts = val
            ).addValueDependency(GRADUAL_HEALTH_REGENERATION, true, true, false)
    );

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
        sizes.put(Items.BEETROOT, 64);
        sizes.put(Items.DRIED_KELP, 64);

        sizes.put(Items.COOKIE, 32);
        sizes.put(Items.GLOW_BERRIES, 32);
        sizes.put(Items.MELON_SLICE, 32);
        sizes.put(Items.SWEET_BERRIES, 32);

        sizes.put(Items.APPLE, 16);
        sizes.put(Items.ENCHANTED_GOLDEN_APPLE, 16);
        sizes.put(Items.GOLDEN_APPLE, 16);
        sizes.put(Items.POTATO, 16);
        sizes.put(Items.BAKED_POTATO, 16);
        sizes.put(Items.POISONOUS_POTATO, 16);
        sizes.put(Items.CARROT, 16);
        sizes.put(Items.GOLDEN_CARROT, 16);
        sizes.put(Items.CHORUS_FRUIT, 16);

        sizes.put(Items.BREAD, 8);
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

        sizes.put(Items.BEEF, 4);
        sizes.put(Items.COOKED_BEEF, 4);
        sizes.put(Items.PORKCHOP, 4);
        sizes.put(Items.COOKED_PORKCHOP, 4);
        sizes.put(Items.PUMPKIN_PIE, 4);

        sizes.put(Items.BEETROOT_SOUP, 1);
        sizes.put(Items.MUSHROOM_STEW, 1);
        sizes.put(Items.RABBIT_STEW, 1);
        sizes.put(Items.SUSPICIOUS_STEW, 1);

        var result = new LinkedHashMap<String, Integer>();
        for (var entry : sizes.entrySet()) {
            result.put(Registries.ITEM.getId(entry.getKey()).toString(), entry.getValue());
        }
        return result;
    }

    private static final ItemIntegerMapConfigOption CUSTOM_FOOD_STACK_SIZES = (ItemIntegerMapConfigOption) new ItemIntegerMapConfigOption(
            "customFoodStackSizes", getDefaultItemStackSizes(), true, true,
            () -> serverData.customFoodStackSizes, val -> serverData.customFoodStackSizes = val
    ).addDependency(USE_CUSTOM_FOOD_STACK_SIZES, true);

    private static final ConfigOption<Boolean, NullType> HIDE_EXPERIENCE_BAR = new ConfigOption<>(
            "hideExperienceBar", true, true, false,
            () -> clientData.hideExperienceBar, val -> clientData.hideExperienceBar = val
    );

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_IN_INVENTORY = (
            new ConfigOption<Boolean, Boolean>(
                    "showExperienceInInventory", true, false, false,
                    () -> clientData.showExperienceInInventory, val -> clientData.showExperienceInInventory = val
            ).addValueDependency(HIDE_EXPERIENCE_BAR, true, true, false)
    );

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_ON_SCREENS = new ConfigOption<Boolean, Boolean>(
            "showExperienceOnScreens", true, false, false,
            () -> clientData.showExperienceOnScreens, val -> clientData.showExperienceOnScreens = val
    ).addValueDependency(HIDE_EXPERIENCE_BAR, true, true, false);

    private static final ConfigOption<Boolean, Boolean> SHOW_EXPERIENCE_ON_GAIN = new ConfigOption<Boolean, Boolean>(
            "showExperienceOnGain", true, false, false,
            () -> clientData.showExperienceOnGain, val -> clientData.showExperienceOnGain = val
    ).addValueDependency(HIDE_EXPERIENCE_BAR, true, true, false);

    private static final ConfigOption<Boolean, Boolean> ENABLE_EXPERIENCE_ANIMATION = new ConfigOption<Boolean, Boolean>(
            "enableExperienceAnimation", true, false, false,
            () -> clientData.enableExperienceAnimation, val -> clientData.enableExperienceAnimation = val
    ).addValueDependency(HIDE_EXPERIENCE_BAR, true, true, false);

    private static final ConfigOption<Boolean, Boolean> RENDER_EXPERIENCE_OVER_BACKGROUND = new ConfigOption<Boolean, Boolean>(
            "renderExperienceOverBackground", true, false, false,
            () -> clientData.renderExperienceOverBackground, val -> clientData.renderExperienceOverBackground = val
    ).addValueDependency(HIDE_EXPERIENCE_BAR, true, true, false);

    private static BooleanControllerBuilder createBooleanController(Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).yesNoFormatter().coloured(true);
    }

    private static ConfigCategory buildHungerCategory() {
        var builder = ConfigCategory.createBuilder()
                .name(Text.translatable(CONFIG_PREFIX + "hunger_category_name"))
                .tooltip(Text.translatable(CONFIG_PREFIX + "hunger_category_tooltip"))
                .option(DISABLE_HUNGER.buildYACLOption(YACLConfig::createBooleanController));
        if (MelancholicHunger.nostalgicTweaksInstalled) {
            builder.option(HIDE_HUNGER_BAR.buildYACLOption(YACLConfig::createBooleanController));
        }
        builder
                .option(HUNGER_EFFECT.buildYACLOption(
                        option -> EnumControllerBuilder.create(option).enumClass(HungerEffectOption.class)
                                .formatValue(
                                        value -> switch (value) {
                                            case VANILLA ->
                                                    Text.translatable(CONFIG_PREFIX + "hunger_effect_vanilla_option")
                                                            // green
                                                            .setStyle(Style.EMPTY.withColor(5635925));
                                            case DISABLED ->
                                                    Text.translatable(CONFIG_PREFIX + "hunger_effect_disabled_option")
                                                            // red
                                                            .setStyle(Style.EMPTY.withColor(16733525));
                                            case REPLACED_WITH_POISON ->
                                                    Text.translatable(CONFIG_PREFIX + "hunger_effect_replaced_with_poison_option")
                                                            // yellow
                                                            .setStyle(Style.EMPTY.withColor(16777045));
                                        }
                                )
                ))
                .option(HIGHLIGHT_RESTORED_HEARTS.buildYACLOption(YACLConfig::createBooleanController))
                .option(GRADUAL_HEALTH_REGENERATION.buildYACLOption(YACLConfig::createBooleanController))
                .option(GRADUAL_HEALTH_REGENERATION_SPEED.buildYACLOption(
                        option -> FloatSliderControllerBuilder.create(option).range(0.1F, 10.0F).step(0.1F)
                ))
                .option(HIGHLIGHT_REGENERATED_HEARTS.buildYACLOption(YACLConfig::createBooleanController));

        return builder.build();
    }

    private static ConfigCategory buildFoodItemsCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable(CONFIG_PREFIX + "food_category_name"))
                .tooltip(Text.translatable(CONFIG_PREFIX + "food_category_tooltip"))
                .option(USE_CUSTOM_FOOD_STACK_SIZES.buildYACLOption(YACLConfig::createBooleanController))
                .option(CUSTOM_FOOD_STACK_SIZES.buildYACLOption())
                .build();
    }

    private static ConfigCategory buildSprintingCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable(CONFIG_PREFIX + "sprinting_category_name"))
                .tooltip(Text.translatable(CONFIG_PREFIX + "sprinting_category_tooltip"))
                .option(SPRINTING.buildYACLOption(
                        option -> EnumControllerBuilder.create(option).enumClass(SprintingOption.class)
                                .formatValue(
                                        value -> switch (value) {
                                            case VANILLA ->
                                                    Text.translatable(CONFIG_PREFIX + "sprinting_vanilla_option")
                                                            // green
                                                            .setStyle(Style.EMPTY.withColor(5635925));
                                            case DISABLED ->
                                                    Text.translatable(CONFIG_PREFIX + "sprinting_disabled_option")
                                                            // red
                                                            .setStyle(Style.EMPTY.withColor(16733525));
                                            case LIMITED_BY_HEALTH ->
                                                    Text.translatable(CONFIG_PREFIX + "sprinting_limited_by_health_option")
                                                            // yellow
                                                            .setStyle(Style.EMPTY.withColor(16777045));
                                        }
                                )
                ))
                .option(SPRINTING_HEALTH_LIMIT.buildYACLOption(
                        option -> IntegerSliderControllerBuilder.create(option).range(1, 20).step(1)
                ))
                .build();
    }

    private static ConfigCategory buildExperienceCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.translatable(CONFIG_PREFIX + "experience_category_name"))
                .tooltip(Text.translatable(CONFIG_PREFIX + "experience_category_tooltip"))
                .option(HIDE_EXPERIENCE_BAR.buildYACLOption(YACLConfig::createBooleanController))
                .option(SHOW_EXPERIENCE_IN_INVENTORY.buildYACLOption(YACLConfig::createBooleanController))
                .option(SHOW_EXPERIENCE_ON_SCREENS.buildYACLOption(YACLConfig::createBooleanController))
                .option(SHOW_EXPERIENCE_ON_GAIN.buildYACLOption(YACLConfig::createBooleanController))
                .option(ENABLE_EXPERIENCE_ANIMATION.buildYACLOption(YACLConfig::createBooleanController))
                .option(RENDER_EXPERIENCE_OVER_BACKGROUND.buildYACLOption(YACLConfig::createBooleanController))
                .build();
    }

    public static YetAnotherConfigLib getYACLInstance() {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Text.translatable(CONFIG_PREFIX + "title"))
                .category(buildHungerCategory())
                .category(buildFoodItemsCategory())
                .category(buildSprintingCategory())
                .category(buildExperienceCategory())
                .save(() -> {
                    var client = MinecraftClient.getInstance();
                    boolean isSinglePlayer = client.isInSingleplayer();
                    var player = client.player;
                    if (!MelancholicHunger.nostalgicTweaksInstalled) {
                        // hideHungerBar is hidden when NT is not installed, so we have to check that it has the correct value
                        HIDE_HUNGER_BAR.updateValueAccordingToDependency();
                    }
                    if (isSinglePlayer || player == null) {
                        // writing config file if in singleplayer or if on title screen
                        HANDLER.save();
                    }
                    else {
                        // sending config to the server if in multiplayer
                        PlayerComponents.SERVER_CONFIG.get(player).sendToServer(serverData.getImmutable());
                    }
                })
        );
    }

    public static void loadFromDisk() {
        HANDLER.load();
        for (var option : List.of(
                DISABLE_HUNGER, GRADUAL_HEALTH_REGENERATION, GRADUAL_HEALTH_REGENERATION_SPEED, HIDE_HUNGER_BAR,
                HUNGER_EFFECT, HIGHLIGHT_REGENERATED_HEARTS, USE_CUSTOM_FOOD_STACK_SIZES, CUSTOM_FOOD_STACK_SIZES, SPRINTING,
                SPRINTING_HEALTH_LIMIT, HIGHLIGHT_RESTORED_HEARTS, HIDE_EXPERIENCE_BAR, SHOW_EXPERIENCE_IN_INVENTORY,
                SHOW_EXPERIENCE_ON_SCREENS, SHOW_EXPERIENCE_ON_GAIN, ENABLE_EXPERIENCE_ANIMATION, RENDER_EXPERIENCE_OVER_BACKGROUND
        )) {
            option.validateValue();
        }
    }

    public static void saveToDisk() {
        HANDLER.save();
    }

    public static void setClientData(ClientConfigData.ImmutableClientConfigData newClientData) {
        HIDE_HUNGER_BAR.setValue(newClientData.hideHungerBar());
        HIGHLIGHT_REGENERATED_HEARTS.setValue(newClientData.highlightRegeneratedHearts());
        HIGHLIGHT_RESTORED_HEARTS.setValue(newClientData.highlightRestoredHearts());
        HIDE_EXPERIENCE_BAR.setValue(newClientData.hideExperienceBar());
        SHOW_EXPERIENCE_IN_INVENTORY.setValue(newClientData.showExperienceInInventory());
        SHOW_EXPERIENCE_ON_SCREENS.setValue(newClientData.showExperienceOnScreens());
        SHOW_EXPERIENCE_ON_GAIN.setValue(newClientData.showExperienceOnScreens());
        ENABLE_EXPERIENCE_ANIMATION.setValue(newClientData.enableExperienceAnimation());
        RENDER_EXPERIENCE_OVER_BACKGROUND.setValue(newClientData.renderExperienceOverBackground());
    }

    public static ServerConfigData.ImmutableServerConfigData getServerData() {
        return serverData.getImmutable();
    }

    public static boolean setServerData(ServerConfigData.ImmutableServerConfigData newServerData) {
        if (serverData.getImmutable().equals(newServerData)) {
            return false;
        }
        DISABLE_HUNGER.setValue(newServerData.disableHunger());
        HUNGER_EFFECT.setValue(newServerData.hungerEffect());
        GRADUAL_HEALTH_REGENERATION.setValue(newServerData.gradualHealthRegeneration());
        GRADUAL_HEALTH_REGENERATION_SPEED.setValue(newServerData.gradualHealthRegenerationSpeed());
        USE_CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.useCustomFoodStackSizes());
        CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.customFoodStackSizes());
        SPRINTING.setValue(newServerData.sprinting());
        SPRINTING_HEALTH_LIMIT.setValue(newServerData.sprintingHealthLimit());
        return true;
    }

    public static boolean disableHunger() {
        return serverData.disableHunger;
    }
    public static boolean hideHungerBar() {
        return clientData.hideHungerBar;
    }
    public static boolean highlightRegeneratedHearts() {
        return clientData.highlightRegeneratedHearts;
    }
    public static boolean highlightRestoredHearts() {
        return clientData.highlightRestoredHearts;
    }
    public static HungerEffectOption hungerEffect() {
        return serverData.hungerEffect;
    }
    public static boolean gradualHealthRegeneration() {
        return serverData.gradualHealthRegeneration;
    }
    public static float gradualHealthRegenerationSpeed() {
        return serverData.gradualHealthRegenerationSpeed;
    }
    public static Integer getItemStackSize(ItemStack itemStack) {
        var itemId = Registries.ITEM.getId(itemStack.getItem()).toString();
        if (serverData.useCustomFoodStackSizes && serverData.customFoodStackSizes.containsKey(itemId)) {
            return serverData.customFoodStackSizes.get(itemId);
        }
        else if (customItemStackSizes.containsKey(itemId)) {
            return customItemStackSizes.get(itemId);
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
    public static boolean enableExperienceAnimation() {return clientData.enableExperienceAnimation;}
    public static boolean renderExperienceOverBackground() {return clientData.renderExperienceOverBackground;}
}
