package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.PlayerComponents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Final @Shadow private Abilities abilities;

    private PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world)
    {
        super(entityType, world);
    }

    /**
     * Doesn't allow for the player to eat food if health is full
     */
    @WrapMethod(method = "canEat")
    private boolean melancholic_hunger$canPlayerEatFood(boolean ignoreHunger, Operation<Boolean> original) {
        if (this.abilities.invulnerable || ignoreHunger) {
            return true;
        }
        return PlayerComponents.HEALTH_REGENERATION.get(this).canEat();
    }
}
