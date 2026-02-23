package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.InstalledMods;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
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

        boolean isPendingValueEqualsRequired() {
            return configOption.YACLOption.pendingValue().equals(requiredValue);
        }

        Component getDependentOptionDescription() {
            var value = configOption.YACLOption.pendingValue();
            if (value instanceof Boolean valueBool) {
                return valueBool ?
                        Component.translatable(CONFIG_PREFIX + "dependency_required_value_enabled") :
                        Component.translatable(CONFIG_PREFIX + "dependency_required_value_not_enabled");
            }
            return Component.translatable(
                    CONFIG_PREFIX + "dependency_required_value_not_set_to",
                    requiredValue == SprintingOption.LIMITED_BY_HEALTH
                            ? Component.translatable(CONFIG_PREFIX + "sprinting_limited_by_health_option")
                            : requiredValue.toString()
            );
        }
    }

    protected final String name;
    private final T defaultValue;
    private final boolean nostalgicTweaksRelated;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    protected Option<T> YACLOption;
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
        return client.isSingleplayer() || player == null || player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
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

    protected OptionDescription buildOptionDescription(T value) {
        var descriptionBuilder = OptionDescription.createBuilder().text(
                Component.literal("\n"), Component.translatable(OPTION_CONFIG_PREFIX + name + ".description")
        );
        if (InstalledMods.NOSTALGIC_TWEAKS && nostalgicTweaksRelated) {
            descriptionBuilder.text(
                    Component.literal("\n"),
                    Component.translatable(CONFIG_PREFIX + "nostalgic_tweaks_controlled_option")
                            .setStyle(Style.EMPTY.withColor(9868950).withItalic(true))
            );
        }
        if (!playerHasPermission) {
            descriptionBuilder.text(
                    Component.literal("\n"),
                    Component.translatable(CONFIG_PREFIX + "op_privileges_required_option")
                            .setStyle(Style.EMPTY.withColor(16733525).withItalic(true))
            );
        }
        else if (dependency != null && !dependency.isPendingValueEqualsRequired()) {
            descriptionBuilder.text(
                    Component.literal("\n"),
                    Component.translatable(
                            CONFIG_PREFIX + "dependency_required_option",
                            Component.translatable(OPTION_CONFIG_PREFIX + dependency.configOption.name + ".name"),
                            dependency.getDependentOptionDescription()
                    ).setStyle(Style.EMPTY.withColor(15118857).withItalic(true))
            );
        }
        return descriptionBuilder.build();
    }

    private void addDependencyListeners() {
        if (dependency == null) {
            return;
        }
        var dependencyYACLOption = dependency.configOption.YACLOption;
        if (valueOnDependencyTrue != null || valueOnDependencyFalse != null) {
            // Updating value of the current option if dependency value changes
            dependencyYACLOption.addEventListener(
                    (option, event) -> {
                        if (event != OptionEventListener.Event.STATE_CHANGE) {
                            return;
                        }
                        var new_value = dependency.isPendingValueEqualsRequired()
                                ? valueOnDependencyTrue : valueOnDependencyFalse;
                        if (new_value != null) {
                            YACLOption.requestSet(new_value);
                        }
                    }
            );
        }
        // Making current option unavailable if dependency value differs from the provided dependencyValue
        dependencyYACLOption.addEventListener(
                (option, event) -> {
                    if (event != OptionEventListener.Event.STATE_CHANGE) {
                        return;
                    }
                    var currentValue = YACLOption.pendingValue();
                    YACLOption.setAvailable(dependency.isPendingValueEqualsRequired());
                    YACLOption.requestSet(currentValue);
                }
        );
    }

    protected boolean getOptionAvailability() {
        if (isServerOption) {
            playerHasPermission = getPlayerHasPermission();
        }
        if (playerHasPermission) {
            if (dependency == null) {
                return true;
            }
            addDependencyListeners();
            return dependency.isCurrentValueEqualsRequired();
        }
        return false;
    }

    public Option<T> buildYACLOption(Function<Option<T>, ControllerBuilder<T>> controllerBuilder) {
        YACLOption = Option.<T>createBuilder()
                .name(Component.translatable(OPTION_CONFIG_PREFIX + name + ".name"))
                .binding(defaultValue, getter, setter)
                .controller(controllerBuilder)
                .available(getOptionAvailability())
                .description(this::buildOptionDescription)
                .build();
        return YACLOption;
    }

    public void forgetPendingValueIfServerOption() {
        if (isServerOption && YACLOption != null) {
            YACLOption.forgetPendingValue();
        }
    }
}
