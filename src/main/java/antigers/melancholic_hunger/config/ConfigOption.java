package antigers.melancholic_hunger.config;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ConfigOption<T, U> {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";
    protected static final String OPTION_CONFIG_PREFIX = CONFIG_PREFIX + "option.";

    public record ConfigOptionDependency<U>(ConfigOption<U, ?> configOption, U requiredValue) {
        boolean isCurrentValueEqualsRequired() {
            U value = configOption.getter.get();
            if (value == null) {
                configOption.validateValue();
                value = configOption.getter.get();
            }
            return requiredValue.equals(value);
        }
    }

    protected final String name;
    private final T defaultValue;
    private final boolean nostalgicTweaksRelated;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    private final boolean isServerOption;
    private boolean playerHasPermission;

    private final ArrayList<ConfigOption<?, T>> dependents = new ArrayList<>();
    @Nullable private ConfigOptionDependency<?> dependency;
    @Nullable private T valueOnDependencyTrue;
    @Nullable private T valueOnDependencyFalse;

    public ConfigOption(
            String name, T defaultValue, boolean nostalgicTweaksRelated, boolean isServerOption,
            Supplier<T> getter, Consumer<T> setter
    ) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.nostalgicTweaksRelated = nostalgicTweaksRelated;
        this.getter = getter;
        this.setter = setter;
        this.isServerOption = isServerOption;
        playerHasPermission = true;
    }

    public static boolean getPlayerHasPermission() {
        var client = Minecraft.getInstance();
        var player = client.player;
        return client.isLocalServer() || player == null || player.hasPermissions(2);
    }

    protected void setValueToDefault() {
        setter.accept(defaultValue);
    }

    public void validateValue() {
        if (getter.get() == null) {
            setValueToDefault();
        }
        updateValueAccordingToDependency();
    }

    protected void updateValueAccordingToDependency() {
        if (dependency != null && !dependency.isCurrentValueEqualsRequired()) {
            setValueForced(valueOnDependencyFalse);
        }
    }

    protected void updateDependents() {
        for (var dependent : dependents) {
            dependent.updateValueAccordingToDependency();
        }
    }

    private void setValueForced(T value) {
        if (value == null || getter.get() == value) {
            return;
        }
        setter.accept(value);
        updateDependents();
    }

    public void setValue(T value) {
        // option can only be set from outside if it is not locked by its dependency
        if (dependency == null || dependency.isCurrentValueEqualsRequired()) {
            setValueForced(value);
        }
    }

    public ConfigOption<T, U> addValueDependency(
            ConfigOption<U, ?> configOption, U requiredValue, T valueOnTrue, T valueOnFalse
    ) {
        this.valueOnDependencyTrue = valueOnTrue;
        this.valueOnDependencyFalse = valueOnFalse;
        return addDependency(configOption, requiredValue);
    }

    public ConfigOption<T, U> addDependency(ConfigOption<U, ?> dependencyOption, U requiredValue) {
        this.dependency = new ConfigOptionDependency<>(dependencyOption, requiredValue);
        dependencyOption.dependents.add(this);
        return this;
    }

    protected boolean getOptionAvailability() {
        if (isServerOption) {
            playerHasPermission = getPlayerHasPermission();
        }
        if (playerHasPermission) {
            if (dependency == null) {
                return true;
            }
            return dependency.isCurrentValueEqualsRequired();
        }
        return false;
    }
}
