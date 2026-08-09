package antigers.melancholic_hunger.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HideHungerBarOption extends ConfigOption<Boolean, Boolean> {
    public HideHungerBarOption(
            String name, Boolean defaultValue, boolean nostalgicTweaksRelated, boolean isServerOption,
            Supplier<Boolean> getter, Consumer<Boolean> setter
    ) {
        super(name, defaultValue, nostalgicTweaksRelated, isServerOption, getter, setter);
    }

    @Override
    public Option<Boolean> buildYACLOption(Function<Option<Boolean>, ControllerBuilder<Boolean>> controllerBuilder) {
        super.buildYACLOption(controllerBuilder);
        dependency.configOption().YACLOption.addEventListener((_, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE && dependency.isCurrentValueEqualsRequired()) {
                YACLOption.requestSet(false);
            }
        });
        return YACLOption;
    }
}
