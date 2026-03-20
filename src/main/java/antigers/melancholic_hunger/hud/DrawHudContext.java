package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.config.SprintingOption;
import antigers.melancholic_hunger.config.MelancholicConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.adrenix.nostalgic.helper.gameplay.stamina.StaminaRenderer;
import mod.adrenix.nostalgic.tweak.config.CandyTweak;
import mod.adrenix.nostalgic.tweak.config.GameplayTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

public class DrawHudContext extends GuiGraphics {
    private final int offsetX;
    private final int healthBarY;
    private final int hudExperienceOffset;
    private int bubblesBarY;
    private int armorBarY;
    private final RestoredHeartsDrawHelper restoredHeartsDrawHelper;
    public static boolean isDefaultArmorHudTexture;
    private final boolean hasMountHealth;
    private final int mountHealthRows;
    private final boolean playerHasArmor;
    private final boolean shouldRenderStamina;
    private final boolean shouldRenderStaminaInPlaceOfHunger;
    private int staminaBarY;
    private boolean isStaminaRenderingActuallyHappening = false;

    public DrawHudContext(
            Minecraft client, PoseStack matrices, MultiBufferSource.BufferSource vertexConsumers,
            RestoredHeartsDrawHelper restoredHeartsDrawHelper, int hudExperienceOffset, boolean hasMountHealth,
            int mountHealthRows
    ) {
        super(client, matrices, vertexConsumers);
        this.restoredHeartsDrawHelper = restoredHeartsDrawHelper;
        var windowWidth = this.guiWidth();
        // fixing offset for odd window width value because vanilla code does integer division by 2 when
        // calculating the x coordinate
        offsetX = windowWidth - ((windowWidth % 2 == 0) ? 9 : 10);
        healthBarY = this.guiHeight() - 32 - hudExperienceOffset;
        this.hudExperienceOffset = hudExperienceOffset;
        this.hasMountHealth = hasMountHealth;
        this.mountHealthRows = mountHealthRows;
        playerHasArmor = client.player.getArmorValue() > 0;
        boolean staminaIsEnabled = InstalledMods.NOSTALGIC_TWEAKS && MelancholicConfig.sprinting() != SprintingOption.DISABLED && GameplayTweak.STAMINA_SPRINT.get();
        shouldRenderStamina = staminaIsEnabled && StaminaRenderer.isVisible();
        shouldRenderStaminaInPlaceOfHunger = staminaIsEnabled && !CandyTweak.HIDE_STAMINA_BAR.get() && !CandyTweak.HIDE_STAMINA_BAR_INACTIVE.get();
    }

    public RestoredHeartsDrawHelper getHelper() {
        return restoredHeartsDrawHelper;
    }

    public int getHudExperienceOffset() {
        return hudExperienceOffset;
    }

    public void prepareArmorAndBubblesBarsDrawing(int aboveHealthY) {
        if (hasMountHealth) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing bubbles above all mount health rows
            bubblesBarY = aboveHealthY - 10 * (mountHealthRows - 1);
        }
        else if (!MelancholicConfig.hideHungerBar()) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            if (shouldRenderStamina) {
                // drawing staminaBarY above the hunger bar
                staminaBarY = healthBarY - 10;
                // drawing bubbles above the stamina bar
                bubblesBarY = healthBarY - 20;
            } else {
                // drawing bubbles above the hunger bar
                bubblesBarY = healthBarY - 10;
            }
        }
        else if (shouldRenderStamina && shouldRenderStaminaInPlaceOfHunger) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing staminaBarY in place of hunger bar (same height as health)
            staminaBarY = healthBarY;
            // drawing bubbles above the stamina bar
            bubblesBarY = healthBarY - 10;
        }
        else {
            // drawing armor in place of hunger bar (same height as health)
            armorBarY = healthBarY;
            // drawing staminaBarY above the armor bar (if player has armor)
            staminaBarY = playerHasArmor ? healthBarY - 10 : healthBarY;
            // drawing bubbles above all health rows
            bubblesBarY = aboveHealthY;
        }
    }

    public int getArmorBarY() {
        return armorBarY;
    }

    public int getMirroredX(int x) {
        return offsetX - x;
    }

    public int getHealthBarY() {
        return healthBarY;
    }

    public int getBubblesBarY() {
        return bubblesBarY;
    }

    public boolean getHasMountHealth() {
        return hasMountHealth;
    }

    public boolean getShouldRenderStamina() {
        return shouldRenderStamina;
    }

    public boolean getShouldRenderStaminaInPlaceOfHunger() {
        return shouldRenderStaminaInPlaceOfHunger;
    }

    public boolean getIsStaminaRenderingActuallyHappening() {
        return isStaminaRenderingActuallyHappening;
    }

    public void renderStamina() {
        if (shouldRenderStamina) {
            // we have to check that isStaminaRenderingActuallyHappening is True in StaminaRendererMixin, so that
            // stamina doesn't render in any other context
            isStaminaRenderingActuallyHappening = true;
            StaminaRenderer.render(this, guiHeight() - staminaBarY);
            isStaminaRenderingActuallyHappening = false;
        }
    }
}
