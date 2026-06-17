package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.MelancholicHunger;
import antigers.melancholic_hunger.components.PlayerComponents;
import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.config.SprintingOption;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class RestoredHeartsDrawHelper {
    public record RenderedHeart(ResourceLocation texture, boolean isAtlasTexture, Color color) {}

    public static final ResourceLocation WHITE_FULL_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "white_full_heart_texture"
    );
    public static final ResourceLocation WHITE_HALF_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "white_half_heart_texture"
    );
    public static final ResourceLocation WHITE_RIGHT_HALF_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "white_right_half_heart_texture"
    );
    public static final ResourceLocation ORIGINAL_RIGHT_HALF_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "original_right_half_heart_texture"
    );
    public static final ResourceLocation BLINKING_RIGHT_HALF_HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "blinking_right_half_heart_texture"
    );

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
    private final Gui.HeartType heartType;
    private final boolean isHardcore;
    private Function<Boolean, RenderedHeart> restoredHeartGetter;
    private Supplier<RenderedHeart> restoredRightHalfHeartGetter;
    private Function<Boolean, RenderedHeart> regeneratedHeartGetter;

    public RestoredHeartsDrawHelper(Player player, RandomSource random) {
        heartType = Gui.HeartType.forPlayer(player);
        isHardcore = player.level().getLevelData().isHardcore();
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
            Color regeneratingHeartColor = new Color(
                    regeneratedHeartsOverlayColor.getRed(), regeneratedHeartsOverlayColor.getGreen(), regeneratedHeartsOverlayColor.getBlue(),
                    (int)Math.floor(calculateRegeneratingHeartOpacity() * 255F)
            );
            switch (MelancholicConfig.regeneratedHeartsTexture()) {
                case SINGLE_COLOR -> regeneratedHeartGetter = isHalf -> getWhiteHeart(isHalf, regeneratingHeartColor);
                case ORIGINAL -> regeneratedHeartGetter = isHalf -> getAtlasSpriteHeart(heartType, isHalf, false, regeneratingHeartColor);
                case BLINKING -> regeneratedHeartGetter = isHalf -> getAtlasSpriteHeart(heartType, isHalf, true, regeneratingHeartColor);
            }
        }

        highlightRestoredHearts = disableHunger && MelancholicConfig.highlightRestoredHearts();
        if (highlightRestoredHearts) {
            Color restoredHeartsOverlayColor = MelancholicConfig.restoredHeartsOverlayColor();
            switch (MelancholicConfig.restoredHeartsTexture()) {
                case SINGLE_COLOR -> {
                    restoredHeartGetter = isHalf -> getWhiteHeart(isHalf, restoredHeartsOverlayColor);
                    restoredRightHalfHeartGetter = () -> new RenderedHeart(WHITE_RIGHT_HALF_HEART_TEXTURE, false, restoredHeartsOverlayColor);
                }
                case ORIGINAL -> {
                    restoredHeartGetter = isHalf -> getAtlasSpriteHeart(Gui.HeartType.NORMAL, isHalf, false, restoredHeartsOverlayColor);
                    restoredRightHalfHeartGetter = () -> new RenderedHeart(ORIGINAL_RIGHT_HALF_HEART_TEXTURE, false, restoredHeartsOverlayColor);
                }
                case BLINKING -> {
                    restoredHeartGetter = isHalf -> getAtlasSpriteHeart(Gui.HeartType.NORMAL, isHalf, true, restoredHeartsOverlayColor);
                    restoredRightHalfHeartGetter = () -> new RenderedHeart(BLINKING_RIGHT_HALF_HEART_TEXTURE, false, restoredHeartsOverlayColor);
                }
            }
        }

        consumedNutrition = disableHunger ? player.getData(PlayerComponents.HEALTH_REGENERATION).getConsumedNutrition() : 0;
        heldFoodNutrition = foodComponent != null ? MelancholicConfig.getFoodHealth(heldItemStack, foodComponent) : 0;
        totalNutritionToDraw = highlightRegeneratedHearts ? consumedNutrition + heldFoodNutrition : heldFoodNutrition;
    }

    private RenderedHeart getWhiteHeart(boolean isHalf, Color color) {
        return new RenderedHeart(isHalf ? WHITE_HALF_HEART_TEXTURE : WHITE_FULL_HEART_TEXTURE, false, color);
    }

    private RenderedHeart getAtlasSpriteHeart(Gui.HeartType type, boolean isHalf, boolean isBlinking, Color color) {
        return new RenderedHeart(type.getSprite(isHardcore, isHalf, isBlinking), true, color);
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

    public Pair<RenderedHeart, RenderedHeart> heartsToDraw() {
        int heartsDiff = currentHeart - playerHealth - 1;
        if (heartsDiff < 0) {
            return new Pair<>(null, null);
        }
        if (highlightRegeneratedHearts && consumedNutrition > 0 && heartsDiff <= consumedNutrition) {
            // this heart is regenerating
            boolean isHalf = heartsDiff == consumedNutrition;
            RenderedHeart regeneratingHeart = regeneratedHeartGetter.apply(isHalf);;
            if (isHalf && highlightRestoredHearts && heldFoodNutrition > 0) {
                // the left half of this heart is regenerating and the right half can be restored by held food
                return new Pair<>(regeneratingHeart, restoredRightHalfHeartGetter.get());
            }
            return new Pair<>(regeneratingHeart, null);
        }
        if (highlightRestoredHearts && totalNutritionToDraw > 0 && heartsDiff <= totalNutritionToDraw) {
            // this heart can be restored by held food
            boolean isHalf = heartsDiff == totalNutritionToDraw;
            return new Pair<>(restoredHeartGetter.apply(isHalf), null);
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