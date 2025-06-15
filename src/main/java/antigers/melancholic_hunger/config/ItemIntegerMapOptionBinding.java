package antigers.melancholic_hunger.config;

import dev.isxander.yacl3.api.Binding;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemIntegerMapOptionBinding implements Binding<List<String>> {

    private final LinkedHashMap<String, Integer> defaultValue;
    private final Supplier<List<String>> getter;
    private final Consumer<List<String>> setter;
    private final Function<Map<String, Integer>, List<String>> converter;

    public ItemIntegerMapOptionBinding(
            LinkedHashMap<String, Integer> defaultValue, Supplier<List<String>> getter, Consumer<List<String>> setter,
            Function<Map<String, Integer>, List<String>> converter
    ) {
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
        this.converter = converter;
    }

    @Override
    public void setValue(List<String> value) {
        this.setter.accept(value);
    }

    @Override
    public List<String> getValue() {
        return this.getter.get();
    }

    @Override
    public List<String> defaultValue() {
        return this.converter.apply(defaultValue);
    }
}
