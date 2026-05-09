package antigers.melancholic_hunger.config;

import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.impl.controller.StringControllerBuilderImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Config option that represents a HashMap that has IDs of game items as keys and integers as values.
 * In YACL it's represented as a list of strings, and a colon is used as a separator between the key and the value.
 * Translated items' names are used for displaying in the GUI
 */
public class ItemIntegerMapConfigOption extends ConfigOption<List<String>, Boolean> {
    private static String translationsLanguage = null;
    private static final HashMap<String, Item> translationToItem = new HashMap<>();
    private static final HashMap<String, List<Item>> translationToItemsList = new HashMap<>();

    private final Supplier<LinkedHashMap<String, Integer>> outerGetter;
    private final Consumer<LinkedHashMap<String, Integer>> outerSetter;
    private final LinkedHashMap<String, Integer> mapDefaultValue;

    public ItemIntegerMapConfigOption(
            String name, LinkedHashMap<String, Integer> defaultValue, boolean nostalgicTweaksRelated, boolean isServerOption,
            Supplier<LinkedHashMap<String, Integer>> outerGetter, Consumer<LinkedHashMap<String, Integer>> outerSetter
    ) {
        super(
                name, null, nostalgicTweaksRelated, isServerOption,
                () -> convertMapToListOfStrings(outerGetter.get()),
                (val) -> outerSetter.accept(convertListOfStringsToMap(val))
        );
        this.outerGetter = outerGetter;
        this.outerSetter = outerSetter;
        this.mapDefaultValue = defaultValue;
    }

    private static void updateTranslations() {
        var clientOptions = Minecraft.getInstance().options;
        if (clientOptions == null) {
            return;
        }
        String currentLanguage = clientOptions.languageCode;
        if (translationsLanguage != null && translationsLanguage.equals(currentLanguage)) {
            return;
        }
        translationsLanguage = currentLanguage;
        translationToItem.clear();
        translationToItemsList.clear();
        BuiltInRegistries.ITEM.iterator().forEachRemaining(
                item -> {
                    String key = Component.translatable(item.getDescriptionId()).getString();
                    if (translationToItem.containsKey(key)) {
                        if (translationToItemsList.containsKey(key)) {
                            translationToItemsList.get(key).add(item);
                        }
                        else {
                            translationToItemsList.put(
                                    key, new ArrayList<>(List.of(translationToItem.get(key), item))
                            );
                        }
                    } else {
                        translationToItem.put(key, item);
                    }
                }
        );
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static ArrayList<String> convertMapToListOfStrings(Map<String, Integer> map) {
        if (map == null) {
            return null;
        }
        updateTranslations();
        var result = new ArrayList<String>();
        for (var entry : map.entrySet()) {
            ResourceLocation itemId = ResourceLocation.parse(entry.getKey());
            String translation = Component.translatable(BuiltInRegistries.ITEM.get(itemId).getDescriptionId()).getString();
            if (translationToItemsList.containsKey(translation)) {
                // adding id in parentheses if the translated name duplicates for multiple food items
                long count = translationToItemsList.get(translation).stream()
                        .filter(item -> map.containsKey(getItemId(item)))
                        .count();

                if (count > 1) {
                    translation = String.format("%s (%s)", translation, itemId);
                }
            }
            result.add(String.format("%s: %d", translation, entry.getValue()));
        }
        return result;
    }

    private static String extractFromParentheses(String input) {
        int startIndex = input.indexOf('(');
        int endIndex = input.indexOf(')', startIndex);

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return input.substring(startIndex + 1, endIndex);
        }
        return null;
    }

    private static LinkedHashMap<String, Integer> convertListOfStringsToMap(List<String> list) {
        if (list == null) {
            return null;
        }
        updateTranslations();
        var result = new LinkedHashMap<String, Integer>();
        for (String line : list) {
            String[] splitLine = line.split(":");
            int value = Integer.parseInt(splitLine[splitLine.length - 1].trim());
            String itemId = extractFromParentheses(line);
            if (itemId != null) {
                result.put(itemId, value);
                continue;
            }
            String key = splitLine[0].trim();
            if (translationToItemsList.containsKey(key)) {
                boolean hasFoodItem = false;
                for (Item item : translationToItemsList.get(key)) {
                    // saving the same size for each of the food items that have the same translated name
                    if (item.getDefaultInstance().get(DataComponents.FOOD) != null) {
                        result.put(getItemId(item), value);
                        hasFoodItem = true;
                    }
                }
                if (!hasFoodItem) {
                    // if there is no single food item, saving for every item with the matching translated name
                    translationToItemsList.get(key).forEach(item -> result.put(getItemId(item), value));
                }
            } else if (translationToItem.containsKey(key)) {
                result.put(getItemId(translationToItem.get(key)), value);
            }
        }
        return result;
    }

    protected void setValueToDefault() {
        outerSetter.accept(mapDefaultValue);
    }

    public void validateValue() {
        if (outerGetter.get() == null) {
            setValueToDefault();
        }
    }

    public void setValue(LinkedHashMap<String, Integer> value) {
        if (value == null || outerGetter.get() == value) {
            return;
        }
        outerSetter.accept(value);
    }

    public ListOption<String> buildYACLOption() {
        var option = ListOption.<String>createBuilder()
                .name(Component.translatable(OPTION_CONFIG_PREFIX + name + ".name"))
                .binding(new ItemIntegerMapOptionBinding(
                        mapDefaultValue, getter, setter, ItemIntegerMapConfigOption::convertMapToListOfStrings
                ))
                .controller(StringControllerBuilderImpl::new)
                .initial(": 0")
                .available(getOptionAvailability())
                .description(buildOptionDescription(null))
                .addListener(
                        (opt, event) ->
                                ((CustomYACLListOption)opt).melancholic_hunger$updateDescription(
                                        buildOptionDescription(null)
                                )
                )
                .build();
        YACLOption = option;
        return option;
    }

    public void updatePendingValue(LinkedHashMap<String, Integer> value) {
        YACLOption.requestSet(convertMapToListOfStrings(value));
    }
}
