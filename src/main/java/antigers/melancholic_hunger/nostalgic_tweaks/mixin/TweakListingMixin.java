package antigers.melancholic_hunger.nostalgic_tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mod.adrenix.nostalgic.tweak.factory.TweakListing;
import mod.adrenix.nostalgic.tweak.listing.Listing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TweakListing.class)
public class TweakListingMixin<V, T extends Listing<V, T>> {
    @Shadow(remap=false) @Final private T diskList;

    /**
     * setDisk always clears the value, even if value is the same
     */
    @WrapMethod(method="setDisk(Lmod/adrenix/nostalgic/tweak/listing/Listing;)V", remap = false)
    private void melancholic_hunger$setDisk(T value, Operation<Void> original) {
        if (this.diskList != value) {
            original.call(value);
        }
    }
}
