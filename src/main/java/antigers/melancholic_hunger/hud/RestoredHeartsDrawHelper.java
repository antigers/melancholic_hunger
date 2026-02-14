package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.config.SprintingOption;
import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class RestoredHeartsDrawHelper {
    public record RestoredHeart(InGameHud.HeartType heartType, boolean isHalf, int colorRed, int colorGreen, int colorBlue) {}

    private final int playerHealth;
    private int currentHeart;
    private final int totalNutritionToDraw;
    private final int heldFoodNutrition;
    private final int consumedNutrition;
    private int absorption;
    private int currentY;
    private final Random random;
    private final int sprintingHealthLimit;
    private final boolean highlightRegeneratedHearts;
    private final boolean highlightRestoredHearts;
    private final int regeneratingHeartColor;
    private final InGameHud.HeartType heartType;

    public RestoredHeartsDrawHelper(PlayerEntity player, Random random) {
        regeneratingHeartColor = calculateBlinkingColor();
        heartType = InGameHud.HeartType.fromPlayerState(player);
        playerHealth = MathHelper.ceil(player.getHealth());
        absorption = MathHelper.ceil(player.getAbsorptionAmount());
        var heldItemStack = player.getMainHandStack();
        var foodComponent = heldItemStack.get(DataComponentTypes.FOOD);
        if (foodComponent == null) {
            foodComponent = player.getOffHandStack().get(DataComponentTypes.FOOD);
        }
        currentHeart = MathHelper.ceil(player.getMaxHealth());
        this.random = random;
        sprintingHealthLimit = YACLConfig.sprinting() == SprintingOption.LIMITED_BY_HEALTH ? YACLConfig.sprintingHealthLimit() : 4;
        highlightRegeneratedHearts = YACLConfig.highlightRegeneratedHearts();
        highlightRestoredHearts = YACLConfig.highlightRestoredHearts();
        consumedNutrition = PlayerComponents.HEALTH_REGENERATION.get(player).getConsumedNutrition();
        heldFoodNutrition = foodComponent != null ? YACLConfig.getFoodHealth(heldItemStack, foodComponent) : 0;
        totalNutritionToDraw = highlightRegeneratedHearts ? consumedNutrition + heldFoodNutrition : heldFoodNutrition;
    }

    private int calculateBlinkingColor() {
        return (int)MathHelper.abs(
                MathHelper.sin(
                        (float)(Util.getMeasuringTimeMs() % 1500L) / 1500.0F * (float)(Math.PI * 2)
                ) * 155F
        ) + 50;
    }

    public Pair<RestoredHeart, RestoredHeart> heartsToDraw() {
        int heartsDiff = currentHeart - playerHealth - 1;
        if (heartsDiff < 0) {
            return new Pair<>(null, null);
        }
        if (highlightRegeneratedHearts && consumedNutrition > 0 && heartsDiff <= consumedNutrition) {
            // this heart is regenerating
            boolean isHalf = heartsDiff == consumedNutrition;
            var regeneratingHeart = new RestoredHeart(heartType, isHalf, regeneratingHeartColor, regeneratingHeartColor, regeneratingHeartColor);
            if (isHalf && highlightRestoredHearts && heldFoodNutrition > 0) {
                // the left half of this heart is regenerating and the second half can be restored by held food
                return new Pair<>(
                        new RestoredHeart(InGameHud.HeartType.NORMAL, false, 120, 70, 70),
                        regeneratingHeart
                );
            }
            return new Pair<>(regeneratingHeart, null);
        }
        if (highlightRestoredHearts && totalNutritionToDraw > 0 && heartsDiff <= totalNutritionToDraw) {
            // this heart can be restored by held food
            return new Pair<>(
                    new RestoredHeart(
                            InGameHud.HeartType.NORMAL, heartsDiff == totalNutritionToDraw, 120, 70, 70
                    ),
                    null
            );
        }
        return new Pair<>(null, null);
    }

    public void updateCurrentHeart() {
        if (absorption <= 0) {
            currentHeart -= 2;
        }
        else {
            absorption -= 2;
        }
    }

    public int getCurrentY() {
        return currentY;
    }

    public int addShakingIfNeeded(int y) {
        // making hearts shake when there are 3 or fewer hearts left instead of default 2
        if (playerHealth <= sprintingHealthLimit) {
            return y + this.random.nextInt(2);
        }
        return y;
    }

    public int updateCurrentY(int y) {
        y = addShakingIfNeeded(y);
        currentY = y;
        return y;
    }
}