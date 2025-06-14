package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;

public class DrawHudContext extends DrawContext {
    private final int offsetX;
    private final int healthBarY;
    private final int hudExperienceOffset;
    private int bubblesBarY;
    private int armorBarY;
    private final RestoredHeartsDrawHelper restoredHeartsDrawHelper;
    public static boolean isDefaultArmorHudTexture;
    private final boolean isRiding;

    public DrawHudContext(
            MinecraftClient client, VertexConsumerProvider.Immediate vertexConsumers,
            RestoredHeartsDrawHelper restoredHeartsDrawHelper, int hudExperienceOffset
    ) {
        super(client, vertexConsumers);
        this.restoredHeartsDrawHelper = restoredHeartsDrawHelper;
        var windowWidth = this.getScaledWindowWidth();
        // fixing offset for odd window width value because vanilla code does integer division by 2 when
        // calculating the x coordinate
        offsetX = windowWidth - ((windowWidth % 2 == 0) ? 9 : 10);
        healthBarY = this.getScaledWindowHeight() - 32 - hudExperienceOffset;
        this.hudExperienceOffset = hudExperienceOffset;
        isRiding = client.player.getVehicle() != null;
    }

    public RestoredHeartsDrawHelper getHelper() {
        return restoredHeartsDrawHelper;
    }

    public void prepareArmorAndBubblesBarsDrawing(int healthBarHighestRowY) {
        var aboveHealthY = healthBarHighestRowY - 3 - hudExperienceOffset;
        if (YACLConfig.hideHungerBar()) {
            // drawing armor in place of hunger bar (same height as health)
            armorBarY = isRiding ? aboveHealthY : healthBarY;
            // drawing bubbles above all health rows
            bubblesBarY = aboveHealthY;
        }
        else {
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

    public boolean getIsRiding() {
        return isRiding;
    }
}
