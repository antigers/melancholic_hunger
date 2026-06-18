package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.MelancholicConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

public class DrawHudContext extends GuiGraphics {
    public static boolean isDefaultArmorHudTexture;

    private final int offsetX;
    private final int healthBarY;
    private final int hudExperienceOffset;
    private int bubblesBarY;
    private int armorBarY;
    private final RestoredHeartsDrawHelper restoredHeartsDrawHelper;
    private final boolean hasMountHealth;
    private final int mountHealthRows;

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
        } else if (MelancholicConfig.hideHungerBar()) {
            // drawing armor in place of hunger bar (same height as health)
            armorBarY = healthBarY;
            // drawing bubbles above all health rows
            bubblesBarY = aboveHealthY;
        } else {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing bubbles above the hunger bar
            bubblesBarY = healthBarY - 10;
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
