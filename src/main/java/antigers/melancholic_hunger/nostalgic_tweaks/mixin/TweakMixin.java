package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import mod.adrenix.nostalgic.tweak.factory.Tweak;
import mod.adrenix.nostalgic.tweak.listing.ItemMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Tweak.class)
public abstract class TweakMixin<T> {
    @Shadow(remap=false) private String jsonId;

    @Unique
    Set<String> overwrittenOptions = Set.of(
            "hideHungerBar", "hideExperienceBar", "disableHunger", "preventHungerEffect", "disableSprint",
            "oldFoodStacking", "instantEat"
    );

    @Inject(
        method="get",
        at=@At("RETURN"),
        cancellable=true,
        remap=false
    )
    void makeTweaksConstantlyFalse(CallbackInfoReturnable<T> callback) {
        if (this.jsonId == null) {
            return;
        }
        if (callback.getReturnValue() instanceof Boolean && overwrittenOptions.contains(this.jsonId)) {
            callback.setReturnValue((T) Boolean.FALSE);
        }
    }
}
