package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.nostalgic_tweaks.NostalgicTweaksConfigHandlerWriter;
import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.components.ServerConfigComponent;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.lang.model.type.NullType;
import java.util.*;
import java.util.function.BiConsumer;

public class YACLConfig {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";
    private static boolean isLoadedFromDisk = false;

    @SerialEntry(value = "clientOptions")
    private static ClientConfigData clientData = new ClientConfigData();
    @SerialEntry(value = "serverOptions")
    private static ServerConfigData serverData = new ServerConfigData();

    public static int getFoodHealth(ItemStack itemStack, FoodProperties foodComponent) {
        if (MelancholicHunger.nostalgicTweaksInstalled && GameplayTweak.CUSTOM_FOOD_HEALTH.get().containsItem(itemStack)) {
            return GameplayTweak.CUSTOM_FOOD_HEALTH.get().valueFrom(itemStack);
        }
        return foodComponent.nutrition();
    }

    private static final ConfigClassHandler<YACLConfig> HANDLER = ConfigClassHandler.createBuilder(YACLConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath("melancholic_hunger", "config"))
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

    private static final ConfigOption<Boolean, Boolean> INSTANT_EATING = new ConfigOption<>(
            "instantEating", false, true, true,
            () -> serverData.instantEating, val -> serverData.instantEating = val
    );

    private static final ConfigOption<Boolean, Boolean> SHOW_FOOD_ITEM_TOOLTIPS = (
            new ConfigOption<Boolean, Boolean>(
                    "showFoodItemTooltips", true, false, true,
                    () -> serverData.showFoodItemTooltips, val -> serverData.showFoodItemTooltips = val
            ).addValueDependency(DISABLE_HUNGER, true, true, false)
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
            result.put(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getValue());
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

    private static final List<ConfigOption<?, ?>> ALL_OPTIONS = List.of(
            DISABLE_HUNGER, GRADUAL_HEALTH_REGENERATION, GRADUAL_HEALTH_REGENERATION_SPEED, HIDE_HUNGER_BAR,
            HUNGER_EFFECT, HIGHLIGHT_REGENERATED_HEARTS, INSTANT_EATING, SHOW_FOOD_ITEM_TOOLTIPS, USE_CUSTOM_FOOD_STACK_SIZES,
            CUSTOM_FOOD_STACK_SIZES, SPRINTING, SPRINTING_HEALTH_LIMIT, HIGHLIGHT_RESTORED_HEARTS,
            HIDE_EXPERIENCE_BAR, SHOW_EXPERIENCE_IN_INVENTORY, SHOW_EXPERIENCE_ON_SCREENS, SHOW_EXPERIENCE_ON_GAIN,
            ENABLE_EXPERIENCE_ANIMATION, RENDER_EXPERIENCE_OVER_BACKGROUND
    );

    private static BooleanControllerBuilder createBooleanController(Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).yesNoFormatter().coloured(true);
    }

    private static ConfigCategory buildHungerCategory() {
        var builder = ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "hunger_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "hunger_category_tooltip"))
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
                                                    Component.translatable(CONFIG_PREFIX + "hunger_effect_vanilla_option")
                                                            // green
                                                            .setStyle(Style.EMPTY.withColor(5635925));
                                            case DISABLED ->
                                                    Component.translatable(CONFIG_PREFIX + "hunger_effect_disabled_option")
                                                            // red
                                                            .setStyle(Style.EMPTY.withColor(16733525));
                                            case REPLACED_WITH_POISON ->
                                                    Component.translatable(CONFIG_PREFIX + "hunger_effect_replaced_with_poison_option")
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
                .option(HIGHLIGHT_REGENERATED_HEARTS.buildYACLOption(YACLConfig::createBooleanController))
                .option(INSTANT_EATING.buildYACLOption(YACLConfig::createBooleanController))
                .option(SHOW_FOOD_ITEM_TOOLTIPS.buildYACLOption(YACLConfig::createBooleanController));

        return builder.build();
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
    }

    private static void setAllFoodStacksTo64(YACLScreen screen, ButtonOption button) {
        LinkedHashMap<String, Integer> stacks = getDefaultItemStackSizes();
        stacks.replaceAll((k, v) -> 64);
        CUSTOM_FOOD_STACK_SIZES.updatePendingValue(stacks);
    }

    private static ButtonOption createButtonOption(
            String buttonName, BiConsumer<YACLScreen, ButtonOption> action,
            ConfigOption.ConfigOptionDependency<?> dependency
    ) {
        boolean playerHasPermission = ConfigOption.getPlayerHasPermission();
        var descriptionBuilder = OptionDescription.createBuilder();
        descriptionBuilder.text(Component.translatable(CONFIG_PREFIX + "button." + buttonName + ".description"));
        if (!playerHasPermission) {
            descriptionBuilder.text(
                    Component.literal("\n"),
                    Component.translatable(CONFIG_PREFIX + "op_privileges_required_option")
                            .setStyle(Style.EMPTY.withColor(16733525).withItalic(true))
            );
        }
        ButtonOption buttonOption = ButtonOption.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "button." + buttonName + ".name"))
                .available(ConfigOption.getPlayerHasPermission())
                .description(descriptionBuilder.build()).text(Component.empty()).action(action).build();

        dependency.configOption().YACLOption.addEventListener(
                (option, event) -> {
                    if (event != OptionEventListener.Event.STATE_CHANGE) {
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
                .option(USE_CUSTOM_FOOD_STACK_SIZES.buildYACLOption(YACLConfig::createBooleanController))
                .option(CUSTOM_FOOD_STACK_SIZES.buildYACLOption())
                .option(createButtonOption(
                        "set_all_food_stack_sizes_to_1", YACLConfig::setAllFoodStacksTo1,
                        new ConfigOption.ConfigOptionDependency<>(USE_CUSTOM_FOOD_STACK_SIZES, true)
                ))
                .option(createButtonOption(
                        "set_all_food_stack_sizes_to_64", YACLConfig::setAllFoodStacksTo64,
                        new ConfigOption.ConfigOptionDependency<>(USE_CUSTOM_FOOD_STACK_SIZES, true)
                ))
                .build();
    }

    private static ConfigCategory buildSprintingCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "sprinting_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "sprinting_category_tooltip"))
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
                .build();
    }

    private static ConfigCategory buildExperienceCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(CONFIG_PREFIX + "experience_category_name"))
                .tooltip(Component.translatable(CONFIG_PREFIX + "experience_category_tooltip"))
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
                .title(Component.translatable(CONFIG_PREFIX + "title"))
                .category(buildHungerCategory())
                .category(buildFoodItemsCategory())
                .category(buildSprintingCategory())
                .category(buildExperienceCategory())
                .save(() -> {
                    var client = Minecraft.getInstance();
                    boolean isSinglePlayer = client.isSingleplayer();
                    boolean hasSingleplayerServer = client.hasSingleplayerServer();
                    var player = client.player;
                    if (!MelancholicHunger.nostalgicTweaksInstalled) {
                        // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
                        clientData.hideHungerBar = serverData.disableHunger;
                    }
                    if (isSinglePlayer || player == null || hasSingleplayerServer) {
                        // writing config file if in singleplayer or if on title screen
                        HANDLER.save();
                        if (hasSingleplayerServer) {
                            ServerConfigComponent.syncAllPlayersExceptOf(player.getId());
                        }
                    }
                    else {
                        // sending config to the server if in multiplayer
                        PlayerComponents.SERVER_CONFIG.get(player).sendToServer(serverData.getImmutable());
                    }
                    // writing new settings to nostalgic tweaks config
                    if (MelancholicHunger.nostalgicTweaksInstalled) {
                        var handler = (NostalgicTweaksConfigHandlerWriter) ConfigBuilder.getHandler();
                        handler.melancholic_hunger$writeConfigToNT(
                                YACLConfig.serverData.getImmutable(), YACLConfig.clientData.getImmutable()
                        );
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
        // Here MelancholicHunger.nostalgicTweaksInstalled may not be initialized yet, so we need to check directly
        if (FabricLoader.getInstance().getModContainer("nostalgic_tweaks").isEmpty()) {
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
        HIGHLIGHT_RESTORED_HEARTS.setValue(newClientData.highlightRestoredHearts());
        HIDE_EXPERIENCE_BAR.setValue(newClientData.hideExperienceBar());
        SHOW_EXPERIENCE_IN_INVENTORY.setValue(newClientData.showExperienceInInventory());
        SHOW_EXPERIENCE_ON_SCREENS.setValue(newClientData.showExperienceOnScreens());
        SHOW_EXPERIENCE_ON_GAIN.setValue(newClientData.showExperienceOnScreens());
        ENABLE_EXPERIENCE_ANIMATION.setValue(newClientData.enableExperienceAnimation());
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
        if (!MelancholicHunger.nostalgicTweaksInstalled) {
            // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
            HIDE_HUNGER_BAR.setValue(false);
        }
        HUNGER_EFFECT.setValue(newServerData.hungerEffect());
        GRADUAL_HEALTH_REGENERATION.setValue(newServerData.gradualHealthRegeneration());
        GRADUAL_HEALTH_REGENERATION_SPEED.setValue(newServerData.gradualHealthRegenerationSpeed());
        INSTANT_EATING.setValue(newServerData.instantEating());
        SHOW_FOOD_ITEM_TOOLTIPS.setValue(newServerData.showFoodItemTooltips());
        USE_CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.useCustomFoodStackSizes());
        CUSTOM_FOOD_STACK_SIZES.setValue(newServerData.customFoodStackSizes());
        SPRINTING.setValue(newServerData.sprinting());
        SPRINTING_HEALTH_LIMIT.setValue(newServerData.sprintingHealthLimit());
        updateCurrentScreen();
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
    public static boolean shouldInstantlyEat(Item item) {
        if (!serverData.instantEating) {
            return false;
        }
        if (MelancholicHunger.nostalgicTweaksInstalled) {
            return !GameplayTweak.IGNORED_EDIBLES.get().containsItem(item);
        }
        return true;
    }
    public static boolean showFoodItemTooltips() {
        return serverData.showFoodItemTooltips;
    }
    public static Integer getItemStackSize(ItemStack itemStack) {
        var itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        if (serverData.useCustomFoodStackSizes && serverData.customFoodStackSizes.containsKey(itemId)) {
            return serverData.customFoodStackSizes.get(itemId);
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
