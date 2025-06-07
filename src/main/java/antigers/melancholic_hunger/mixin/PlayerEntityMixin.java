package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.PlayerComponents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Final @Shadow private PlayerAbilities abilities;

    private PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world)
    {
        super(entityType, world);
    }

    /**
     * Doesn't allow for the player to eat food if health is full
     */
    @WrapMethod(method = "canConsume")
    private boolean melancholic_hunger$canPlayerEatFood(boolean ignoreHunger, Operation<Boolean> original) {
        if (this.abilities.invulnerable || ignoreHunger) {
            return true;
        }
        return PlayerComponents.HEALTH_REGENERATION.get(this).canEat();
    }
}
