package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.config.YACLConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public class RestoredHeartsDrawHelper {
    public record RestoredHeart(Gui.HeartType heartType, boolean isHalf, int colorRed, int colorGreen, int colorBlue) {}

    private final int playerHealth;
    private int currentHeart;
    private final int totalNutritionToDraw;
    private final int heldFoodNutrition;
    private final int consumedNutrition;
    private int absorption;
    private int currentY;
    private final RandomSource random;
    private final int sprintingHealthLimit;
    private final boolean highlightRegeneratedHearts;
    private final boolean highlightRestoredHearts;
    private final int regeneratingHeartColor;
    private final Gui.HeartType heartType;

    public RestoredHeartsDrawHelper(Player player, RandomSource random) {
        regeneratingHeartColor = calculateBlinkingColor();
        heartType = Gui.HeartType.forPlayer(player);
        playerHealth = Mth.ceil(player.getHealth());
        absorption = Mth.ceil(player.getAbsorptionAmount());
        var heldItemStack = player.getMainHandItem();
        var foodComponent = heldItemStack.getItem().getFoodProperties();
        if (foodComponent == null) {
            foodComponent = player.getOffhandItem().getItem().getFoodProperties();
        }
        currentHeart = Mth.ceil(player.getMaxHealth());
        this.random = random;
        sprintingHealthLimit = YACLConfig.sprintingHealthLimit();
        highlightRegeneratedHearts = YACLConfig.highlightRegeneratedHearts();
        highlightRestoredHearts = YACLConfig.highlightRestoredHearts();
        consumedNutrition = PlayerComponents.HEALTH_REGENERATION.get(player).getConsumedNutrition();
        heldFoodNutrition = foodComponent != null ? YACLConfig.getFoodHealth(heldItemStack, foodComponent) : 0;
        totalNutritionToDraw = highlightRegeneratedHearts ? consumedNutrition + heldFoodNutrition : heldFoodNutrition;
    }

    private int calculateBlinkingColor() {
        return (int)Mth.abs(
                Mth.sin(
                        (float)(Util.getMillis() % 1500L) / 1500.0F * (float)(Math.PI * 2)
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
                        new RestoredHeart(Gui.HeartType.NORMAL, false, 120, 70, 70),
                        regeneratingHeart
                );
            }
            return new Pair<>(regeneratingHeart, null);
        }
        if (highlightRestoredHearts && totalNutritionToDraw > 0 && heartsDiff <= totalNutritionToDraw) {
            // this heart can be restored by held food
            return new Pair<>(
                    new RestoredHeart(
                            Gui.HeartType.NORMAL, heartsDiff == totalNutritionToDraw, 120, 70, 70
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

    public int updateCurrentY(int y) {
        // making hearts shake when there are 3 or fewer hearts left instead of default 2
        if (playerHealth <= sprintingHealthLimit) {
            y += this.random.nextInt(2);
        }
        currentY = y;
        return y;
    }
}