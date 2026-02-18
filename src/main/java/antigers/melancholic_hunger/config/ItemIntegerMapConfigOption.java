package antigers.melancholic_hunger.config;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

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
    private static final HashMap<String, String> translationsToIds = new HashMap<>();
    private static final HashMap<String, List<String>> translationsToIdsLists = new HashMap<>();

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
        translationsToIds.clear();
        translationsToIdsLists.clear();
        ForgeRegistries.ITEMS.iterator().forEachRemaining(
                item -> {
                    String key = Component.translatable(item.getDescriptionId()).getString();
                    String value = ForgeRegistries.ITEMS.getKey(item).toString();
                    if (translationsToIds.containsKey(key)) {
                        if (translationsToIdsLists.containsKey(key)) {
                            translationsToIdsLists.get(key).add(value);
                        }
                        else {
                            translationsToIdsLists.put(
                                    key, new ArrayList<>(List.of(translationsToIds.get(key), value))
                            );
                        }
                    } else {
                        translationsToIds.put(key, value);
                    }
                }
        );
    }

    private static ArrayList<String> convertMapToListOfStrings(Map<String, Integer> map) {
        if (map == null) {
            return null;
        }
        updateTranslations();
        var result = new ArrayList<String>();
        for (var entry : map.entrySet()) {
            ResourceLocation itemId = ResourceLocation.parse(entry.getKey());
            String translation = Component.translatable(ForgeRegistries.ITEMS.getValue(itemId).getDescriptionId()).getString();
            if (translationsToIdsLists.containsKey(translation)) {
                // adding id in parentheses if the translated name duplicates for multiple items
                translation = String.format("%s (%s)", translation, itemId);
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
            if (translationsToIdsLists.containsKey(key)) {
                for (String translation : translationsToIdsLists.get(key)) {
                    // saving the same size for each of the items that have the same translated name
                    result.put(translation, value);
                }
            } else if (translationsToIds.containsKey(key)) {
                result.put(translationsToIds.get(key), value);
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
        updateValueAccordingToDependency();
    }

    public void setValue(LinkedHashMap<String, Integer> value) {
        if (value == null || outerGetter.get() == value) {
            return;
        }
        outerSetter.accept(value);
        updateDependents();
    }
}
