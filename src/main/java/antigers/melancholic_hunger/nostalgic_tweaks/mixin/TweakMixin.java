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
    @Unique private static final ItemMap<Integer> emptyItemMap = new ItemMap<>(0);
    @Shadow(remap=false) private String jsonId;

    @Unique
    Set<String> overwrittenOptions = Set.of(
            "hideHungerBar", "hideExperienceBar", "disableHunger", "preventHungerEffect", "disableSprint",
            "oldFoodStacking"
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
        else if (this.jsonId.equals("customItemStacking")) {
            callback.setReturnValue((T) emptyItemMap);
        }
    }
}
