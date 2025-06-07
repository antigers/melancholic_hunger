package antigers.melancholic_hunger.config;

import antigers.melancholic_hunger.MelancholicHunger;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.impl.controller.StringControllerBuilderImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class ConfigOption<T, U> {
    private static final String CONFIG_PREFIX = "screen.melancholic_hunger.config.";
    private static final String OPTION_CONFIG_PREFIX = CONFIG_PREFIX + "option.";

    private record ConfigOptionDependency<U>(ConfigOption<U, ?> configOption, U requiredValue) {
        boolean isCurrentValueEqualsRequired() {
            return configOption.getValue().equals(requiredValue);
        }

        boolean isPendingValueEqualsRequired() {
            return configOption.YACLOption.pendingValue().equals(requiredValue);
        }

        Text getDependentOptionDescription() {
            var value = configOption.YACLOption.pendingValue();
            if (value instanceof Boolean valueBool) {
                return valueBool ?
                        Text.translatable(CONFIG_PREFIX + "dependency_required_value_enabled") :
                        Text.translatable(CONFIG_PREFIX + "dependency_required_value_not_enabled");
            }
            return Text.translatable(
                    CONFIG_PREFIX + "dependency_required_value_not_set_to",
                    requiredValue == SprintingOption.LIMITED_BY_HEALTH
                            ? Text.translatable(CONFIG_PREFIX + "sprinting_limited_by_health_option")
                            : requiredValue.toString()
            );
        }
    }

    @FunctionalInterface
    protected interface Getter<T> {
        T run();
    }

    @FunctionalInterface
    protected interface Setter<T> {
        void run(T value);
    }

    private final String name;
    private final T defaultValue;
    private final boolean nostalgicTweaksRelated;
    private final Getter<T> getter;
    private final Setter<T> setter;
    private Option<T> YACLOption;
    private final boolean isServerOption;
    private boolean playerHasPermission;

    private final ArrayList<ConfigOption<?, T>> dependents = new ArrayList<>();
    @Nullable private ConfigOptionDependency<?> dependency;
    @Nullable private T valueOnDependencyTrue;
    @Nullable private T valueOnDependencyFalse;

    public ConfigOption(
            String name, T defaultValue, boolean nostalgicTweaksRelated, boolean isServerOption,
            Getter<T> getter, Setter<T> setter
    ) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.nostalgicTweaksRelated = nostalgicTweaksRelated;
        this.getter = getter;
        this.setter = setter;
        this.isServerOption = isServerOption;
        playerHasPermission = true;
        setValueToDefault();
    }

    private void setValueToDefault() {
        setter.run(defaultValue);
    }

    public void validateValue() {
        if (dependency == null && getter.run() == null) {
            setValueToDefault();
            return;
        }
        updateValueAccordingToDependency();
    }

    public void updateValueAccordingToDependency() {
        if (dependency == null) {
            return;
        }
        T newValue = dependency.isCurrentValueEqualsRequired() ? valueOnDependencyTrue : valueOnDependencyFalse;
        setValueForced(newValue);
    }

    private T getValue() {
        var res = getter.run();
        return res != null ? res : defaultValue;
    }

    private void setValueForced(T value) {
        if (value == null || getter.run() == value) {
            return;
        }
        setter.run(value);
        for (var dependent : dependents) {
            dependent.updateValueAccordingToDependency();
        }
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

    private OptionDescription buildOptionDescription(T value) {
        var descriptionBuilder = OptionDescription.createBuilder().text(
                Text.translatable(OPTION_CONFIG_PREFIX + name + ".description")
        );
        if (MelancholicHunger.nostalgicTweaksInstalled && nostalgicTweaksRelated) {
            descriptionBuilder.text(
                    Text.literal("\n"),
                    Text.translatable(CONFIG_PREFIX + "nostalgic_tweaks_controlled_option")
                            .setStyle(Style.EMPTY.withColor(9868950).withItalic(true))
            );
        }
        if (!playerHasPermission) {
            descriptionBuilder.text(
                    Text.literal("\n"),
                    Text.translatable(CONFIG_PREFIX + "op_privileges_required_option")
                            .setStyle(Style.EMPTY.withColor(16733525).withItalic(true))
            );
        }
        else if (dependency != null && !dependency.isPendingValueEqualsRequired()) {
            descriptionBuilder.text(
                    Text.literal("\n"),
                    Text.translatable(
                            CONFIG_PREFIX + "dependency_required_option",
                            Text.translatable(OPTION_CONFIG_PREFIX + dependency.configOption.name + ".name"),
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

    private boolean getOptionAvailability() {
        if (isServerOption) {
            var client = MinecraftClient.getInstance();
            var player = client.player;
            playerHasPermission = client.isInSingleplayer() || player == null || player.hasPermissionLevel(2);
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
                .name(Text.translatable(OPTION_CONFIG_PREFIX + name + ".name"))
                .binding(
                        defaultValue,
                        this::getValue,
                        newVal -> setter.run(newVal)
                )
                .controller(controllerBuilder)
                .available(getOptionAvailability())
                .description(this::buildOptionDescription)
                .build();
        return YACLOption;
    }

    public ListOption<String> buildStringListYACLOption() {
        var option = ListOption.<String>createBuilder()
                .name(Text.translatable(OPTION_CONFIG_PREFIX + name + ".name"))
                .binding(
                        (List<String>)defaultValue,
                        () -> (List<String>)getter.run(),
                        newVal -> setter.run((T)newVal)
                )
                .controller(StringControllerBuilderImpl::new)
                .initial("\"minecraft:\": 0")
                .available(getOptionAvailability())
                .description(buildOptionDescription(null))
                .addListener(
                        (opt, event) ->
                                ((CustomYACLListOption)opt).melancholic_hunger$updateDescription(
                                        buildOptionDescription(null)
                                )
                )
                .build();
        YACLOption = (Option<T>)option;
        return option;
    }
}
