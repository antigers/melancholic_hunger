package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.config.SprintingOption;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public class RestoredHeartsDrawHelper {
    public record RestoredHeart(Gui.HeartType heartType, boolean isHalf, Color color) {}

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
    private final Color restoredHeartsOverlayColor;
    private final Color regeneratingHeartColor;
    private final Gui.HeartType heartType;

    public RestoredHeartsDrawHelper(Player player, RandomSource random) {
        heartType = Gui.HeartType.forPlayer(player);
        playerHealth = Mth.ceil(player.getHealth());
        absorption = Mth.ceil(player.getAbsorptionAmount());
        var heldItemStack = player.getMainHandItem();
        var foodComponent = heldItemStack.get(DataComponents.FOOD);
        if (foodComponent == null) {
            foodComponent = player.getOffhandItem().get(DataComponents.FOOD);
        }
        currentHeart = Mth.ceil(player.getMaxHealth());
        this.random = random;
        sprintingHealthLimit = MelancholicConfig.sprinting() == SprintingOption.LIMITED_BY_HEALTH ? MelancholicConfig.sprintingHealthLimit() : 4;
        boolean disableHunger = MelancholicConfig.disableHunger();
        highlightRegeneratedHearts = disableHunger && MelancholicConfig.highlightRegeneratedHearts();
        if (highlightRegeneratedHearts) {
            Color regeneratedHeartsOverlayColor = MelancholicConfig.regeneratedHeartsOverlayColor();
            regeneratingHeartColor = new Color(
                    regeneratedHeartsOverlayColor.getRed(), regeneratedHeartsOverlayColor.getGreen(), regeneratedHeartsOverlayColor.getBlue(),
                    (int)Math.floor(calculateRegeneratingHeartOpacity() * 255F)
            );
        }
        else {
            regeneratingHeartColor = null;
        }
        highlightRestoredHearts = disableHunger && MelancholicConfig.highlightRestoredHearts();
        restoredHeartsOverlayColor = highlightRestoredHearts ? MelancholicConfig.restoredHeartsOverlayColor() : null;
        consumedNutrition = disableHunger ? PlayerComponents.HEALTH_REGENERATION.get(player).getConsumedNutrition() : 0;
        heldFoodNutrition = foodComponent != null ? MelancholicConfig.getFoodHealth(heldItemStack, foodComponent) : 0;
        totalNutritionToDraw = highlightRegeneratedHearts ? consumedNutrition + heldFoodNutrition : heldFoodNutrition;
    }

    private float calculateRegeneratingHeartOpacity() {
        float minOpacity = MelancholicConfig.regeneratedHeartsOpacityMin();
        float amplitude = MelancholicConfig.regeneratedHeartsOpacityMax() - minOpacity;
        int period = MelancholicConfig.regeneratedHeartsBlinkingPeriod();
		return Mth.abs(
				Mth.sin(
						(float)(Util.getMillis() % period) / (float)period * (float)(Math.PI * 2)
				) * amplitude
		) + minOpacity;
    }

    public Pair<RestoredHeart, RestoredHeart> heartsToDraw() {
        int heartsDiff = currentHeart - playerHealth - 1;
        if (heartsDiff < 0) {
            return new Pair<>(null, null);
        }
        if (highlightRegeneratedHearts && consumedNutrition > 0 && heartsDiff <= consumedNutrition) {
            // this heart is regenerating
            boolean isHalf = heartsDiff == consumedNutrition;
            var regeneratingHeart = new RestoredHeart(heartType, isHalf, regeneratingHeartColor);
            if (isHalf && highlightRestoredHearts && heldFoodNutrition > 0) {
                // the left half of this heart is regenerating and the second half can be restored by held food
                return new Pair<>(
                        new RestoredHeart(Gui.HeartType.NORMAL, false, restoredHeartsOverlayColor),
                        regeneratingHeart
                );
            }
            return new Pair<>(regeneratingHeart, null);
        }
        if (highlightRestoredHearts && totalNutritionToDraw > 0 && heartsDiff <= totalNutritionToDraw) {
            // this heart can be restored by held food
            return new Pair<>(
                    new RestoredHeart(
                            Gui.HeartType.NORMAL, heartsDiff == totalNutritionToDraw, restoredHeartsOverlayColor
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