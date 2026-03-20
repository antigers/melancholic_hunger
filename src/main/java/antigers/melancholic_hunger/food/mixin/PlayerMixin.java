package antigers.melancholic_hunger.food.mixin;

import antigers.melancholic_hunger.components.HealthRegenerationComponent;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Final @Shadow private Abilities abilities;

    @Shadow
    public abstract FoodData getFoodData();

    @Shadow
    public abstract Abilities getAbilities();

    private PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world)
    {
        super(entityType, world);
    }

    /**
     * Allowing player to sprint only if they have more than 3 hearts (or custom amount)
     */
    @WrapMethod(method = "hasEnoughFoodToDoExhaustiveManoeuvres")
    private boolean melancholic_hunger$canPlayerSprint(Operation<Boolean> original) {
        if (this.getAbilities().mayfly) {
            return true;
        }
        switch (MelancholicConfig.sprinting()) {
            case DISABLED -> {
                return false;
            }
            case LIMITED_BY_HEALTH -> {
                if (this.getHealth() <= MelancholicConfig.sprintingHealthLimit()) {
                    return false;
                }
            }
        }
        if (MelancholicConfig.disableHunger()) {
            return true;
        }
        return this.getFoodData().hasEnoughFood();
    }

    /**
     * Doesn't allow for the player to eat food if health is full
     */
    @WrapMethod(method = "canEat")
    private boolean melancholic_hunger$canPlayerEatFood(boolean ignoreHunger, Operation<Boolean> original) {
        if (this.abilities.invulnerable || ignoreHunger) {
            return true;
        }
        return HealthRegenerationComponent.get((Player)(Object) this).canEat();
    }
}
