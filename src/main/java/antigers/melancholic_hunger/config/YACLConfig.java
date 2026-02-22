package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.InstalledMods;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;
import org.quiltmc.parsers.json.gson.GsonReader;
import org.quiltmc.parsers.json.gson.GsonWriter;
import vectorwing.farmersdelight.common.registry.ModItems;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class YACLConfig {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("melancholic_hunger.json5");
    private static boolean isLoadedFromDisk = false;

    private static ClientConfigData clientData = new ClientConfigData();
    private static ServerConfigData serverData = new ServerConfigData();

    public static int getFoodHealth(ItemStack itemStack, FoodProperties foodProperties) {
        return foodProperties.getNutrition();
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            MelancholicHunger.LOGGER.info("Config file '{}' does not exist. Creating it with default values.", CONFIG_PATH);
            return;
        }
        MelancholicHunger.LOGGER.info("Deserializing config from '{}'", CONFIG_PATH);
        Gson gson = new Gson();
        try (JsonReader jsonReader = JsonReader.json5(CONFIG_PATH)) {
            GsonReader gsonReader = new GsonReader(jsonReader);
            jsonReader.beginObject();

            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                Class<?> type;
                Consumer<Object> fieldSetter;
                if (name.equals("clientData")) {
                    type = ClientConfigData.class;
                    fieldSetter = value -> clientData = (ClientConfigData) value;
                }
                else if (name.equals("serverData")) {
                    type = ServerConfigData.class;
                    fieldSetter = value -> serverData = (ServerConfigData) value;
                }
                else {
                    MelancholicHunger.LOGGER.warn("Found unknown config field '{}'.", name);
                    jsonReader.skipValue();
                    continue;
                }

                JsonElement element;
                try {
                    element = gson.fromJson(gsonReader, JsonElement.class);
                } catch (Exception e) {
                    MelancholicHunger.LOGGER.error("Failed to deserialize config field '{}'. Due to the error state this JSON reader cannot be re-used and loading will be aborted.", name, e);
                    return;
                }

                if (element.isJsonNull()) {
                    MelancholicHunger.LOGGER.warn("Found null value in non-nullable config field '{}'. Leaving field as default and marking as dirty.", name);
                    continue;
                }

                try {
                    fieldSetter.accept(gson.fromJson(element, type));
                } catch (Exception e) {
                    MelancholicHunger.LOGGER.error("Failed to deserialize config field '{}'. Leaving as default.", name, e);
                }
            }

            jsonReader.endObject();
        } catch (IOException e) {
            MelancholicHunger.LOGGER.error("Failed to deserialize config class.", e);
		}
	}

    private static void saveConfig() {
        MelancholicHunger.LOGGER.info("Serializing config to '{}'", CONFIG_PATH);
        Gson gson = new Gson();
        try (StringWriter stringWriter = new StringWriter()) {
            JsonWriter jsonWriter = JsonWriter.json5(stringWriter);
            GsonWriter gsonWriter = new GsonWriter(jsonWriter);
            jsonWriter.beginObject();

            for (Pair<String, ?> field : List.of(new Pair<>("clientData", clientData), new Pair<>("serverData", serverData))) {
                String name = field.getFirst();
                Object value = field.getSecond();
                jsonWriter.name(name);
                JsonElement element;
                try {
                    element = gson.toJsonTree(value, value.getClass());
                } catch (Exception e) {
                    MelancholicHunger.LOGGER.error("Failed to serialize config field '{}'. Serializing as null.", name, e);
                    jsonWriter.nullValue();
                    continue;
                }

                try {
                    gson.toJson(element, gsonWriter);
                } catch (Exception e) {
                    MelancholicHunger.LOGGER.error("Failed to serialize config field '{}'. Due to the error state this JSON writer cannot continue safely and the save will be abandoned.", name, e);
                    return;
                }
            }
            jsonWriter.endObject();
            jsonWriter.flush();
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, stringWriter.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            MelancholicHunger.LOGGER.error("Failed to serialize config.", e);
        }
    }

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

    private static final ConfigOption<Boolean, Boolean> STOP_REGENERATION_AT_FULL_HEALTH = new ConfigOption<Boolean, Boolean>(
            "stopRegenerationAtFullHealth", false, false, true,
            () -> serverData.stopRegenerationAtFullHealth, val -> serverData.stopRegenerationAtFullHealth = val
    ).addDependency(GRADUAL_HEALTH_REGENERATION, true);

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
            result.put(ForgeRegistries.ITEMS.getKey(entry.getKey()).toString(), entry.getValue());
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
            result.put(((RegistryObject) entry.getKey()).getId().toString(), entry.getValue());
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

    private static final ConfigOption<Integer, Boolean> NOURISHMENT_HEALTH_BOOST_COUNT = new ConfigOption<Integer, Boolean>(
            "nourishmentHealthBoostHeartsCount", 3, false, true,
            () -> serverData.nourishmentHealthBoostHeartsCount, val -> serverData.nourishmentHealthBoostHeartsCount = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final ConfigOption<Float, Boolean> NOURISHMENT_REGEN_SPEED_MULTIPLIER = new ConfigOption<Float, Boolean>(
            "nourishmentRegenSpeedMultiplier", 1.5F, false, true,
            () -> serverData.nourishmentRegenSpeedMultiplier, val -> serverData.nourishmentRegenSpeedMultiplier = val
    ).addDependency(DISABLE_HUNGER, true);

    private static final List<ConfigOption<?, ?>> ALL_OPTIONS = List.of(
            DISABLE_HUNGER, GRADUAL_HEALTH_REGENERATION, GRADUAL_HEALTH_REGENERATION_SPEED, STOP_REGENERATION_AT_FULL_HEALTH, HIDE_HUNGER_BAR,
            HUNGER_EFFECT, HIGHLIGHT_REGENERATED_HEARTS, INSTANT_EATING, SHOW_FOOD_ITEM_TOOLTIPS, USE_CUSTOM_FOOD_STACK_SIZES,
            CUSTOM_FOOD_STACK_SIZES, FARMERS_DELIGHT_FOOD_STACK_SIZES, SPRINTING, SPRINTING_HEALTH_LIMIT, HIGHLIGHT_RESTORED_HEARTS,
            HIDE_EXPERIENCE_BAR, SHOW_EXPERIENCE_IN_INVENTORY, SHOW_EXPERIENCE_ON_SCREENS, SHOW_EXPERIENCE_ON_GAIN,
            ENABLE_EXPERIENCE_ANIMATION, RENDER_EXPERIENCE_OVER_BACKGROUND, NOURISHMENT_HEALTH_BOOST_COUNT,
            NOURISHMENT_REGEN_SPEED_MULTIPLIER
    );

    private static void updateCurrentScreen() {}

    public static void loadFromDisk() {
        if (isLoadedFromDisk) {
            return;
        }
        loadConfig();
        for (var option : ALL_OPTIONS) {
            option.validateValue();
        }
        if (!InstalledMods.NOSTALGIC_TWEAKS) {
            // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
            HIDE_HUNGER_BAR.setValue(false);
        }
        isLoadedFromDisk = true;
        saveConfig();
    }

    public static void saveToDisk() {
        saveConfig();
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
        if (!InstalledMods.NOSTALGIC_TWEAKS) {
            // hideHungerBar option is hidden when NT is not installed, so we have to correct its value
            HIDE_HUNGER_BAR.setValue(false);
        }
        HUNGER_EFFECT.setValue(newServerData.hungerEffect());
        GRADUAL_HEALTH_REGENERATION.setValue(newServerData.gradualHealthRegeneration());
        GRADUAL_HEALTH_REGENERATION_SPEED.setValue(newServerData.gradualHealthRegenerationSpeed());
        STOP_REGENERATION_AT_FULL_HEALTH.setValue(newServerData.stopRegenerationAtFullHealth());
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
    public static boolean stopRegenerationAtFullHealth() {
        return serverData.stopRegenerationAtFullHealth;
    }
    public static boolean shouldInstantlyEat(Item item) {
        if (!serverData.instantEating) {
            return false;
        }
        return true;
    }
    public static boolean showFoodItemTooltips() {
        return serverData.showFoodItemTooltips;
    }
    public static Integer getItemStackSize(ItemStack itemStack) {
        var itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
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
    public static boolean enableExperienceAnimation() {return clientData.enableExperienceAnimation;}
    public static boolean renderExperienceOverBackground() {return clientData.renderExperienceOverBackground;}
    public static int nourishmentHealthBoostHeartsCount() {return serverData.nourishmentHealthBoostHeartsCount;}
    public static float nourishmentRegenSpeedMultiplier() {return serverData.nourishmentRegenSpeedMultiplier;}
}
