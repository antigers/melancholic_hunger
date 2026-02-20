package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.config.YACLConfig;
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

<<<<<<< HEAD:src/main/java/antigers/melancholic_hunger/mixin/PlayerEntityMixin.java
    @Shadow
    public abstract FoodData getFoodData();

    @Shadow
    public abstract Abilities getAbilities();

    private PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world)
=======
    private PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world)
>>>>>>> 1.21.10:src/main/java/antigers/melancholic_hunger/mixin/PlayerMixin.java
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
        switch (YACLConfig.sprinting()) {
            case DISABLED -> {
                return false;
            }
            case LIMITED_BY_HEALTH -> {
                if (this.getHealth() <= YACLConfig.sprintingHealthLimit()) {
                    return false;
                }
            }
        }
        if (YACLConfig.disableHunger()) {
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
        return PlayerComponents.HEALTH_REGENERATION.get(this).canEat();
    }
}
