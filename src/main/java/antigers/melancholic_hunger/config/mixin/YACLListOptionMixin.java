package antigers.melancholic_hunger.config.mixin;

import antigers.melancholic_hunger.config.CustomYACLListOption;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.impl.ListOptionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ListOptionImpl.class)
public class YACLListOptionMixin implements CustomYACLListOption {
    @Unique private OptionDescription overriddenDescription;

    @WrapMethod(method = "description", remap = false)
    OptionDescription melancholic_hunger$getDescription(Operation<OptionDescription> original) {
        return overriddenDescription != null ? overriddenDescription : original.call();
    }

    @Override
    public void melancholic_hunger$updateDescription(OptionDescription description) {
        overriddenDescription = description;
    }
}
