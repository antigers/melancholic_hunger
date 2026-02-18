package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.client.Minecraft;

public class DrawHudContext {
    private final int offsetX;
    private final int healthBarY;
    private final int hudExperienceOffset;
    private int bubblesBarY;
    private int armorBarY;
    private final RestoredHeartsDrawHelper restoredHeartsDrawHelper;
    public static boolean isDefaultArmorHudTexture;
    private final boolean hasMountHealth;
    private final int mountHealthRows;

    public DrawHudContext(
            Minecraft client, RestoredHeartsDrawHelper restoredHeartsDrawHelper, int hudExperienceOffset, boolean hasMountHealth,
            int mountHealthRows, int windowWidth, int windowHeight
    ) {
        this.restoredHeartsDrawHelper = restoredHeartsDrawHelper;
        // fixing offset for odd window width value because vanilla code does integer division by 2 when
        // calculating the x coordinate
        offsetX = windowWidth - ((windowWidth % 2 == 0) ? 9 : 10);
        healthBarY = windowHeight - 32 - hudExperienceOffset;
        this.hudExperienceOffset = hudExperienceOffset;
        this.hasMountHealth = hasMountHealth;
        this.mountHealthRows = mountHealthRows;
    }

    public RestoredHeartsDrawHelper getHelper() {
        return restoredHeartsDrawHelper;
    }

    public int getHudExperienceOffset() {
        return hudExperienceOffset;
    }

    public void prepareArmorAndBubblesBarsDrawing(int healthBarHighestRowY) {
        var aboveHealthY = healthBarHighestRowY - 3 - hudExperienceOffset;
        if (hasMountHealth) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing bubbles above all mount health rows
            bubblesBarY = aboveHealthY - 10 * (mountHealthRows - 1);
        }
        else if (!YACLConfig.hideHungerBar()) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing bubbles above the hunger bar
            bubblesBarY = healthBarY - 10;
        }
        else {
            // drawing armor in place of hunger bar (same height as health)
            armorBarY = healthBarY;
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
}
