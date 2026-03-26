package antigers.melancholic_hunger.hud;

import antigers.melancholic_hunger.config.MelancholicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2fStack;

public class DrawHudContext extends GuiGraphicsExtractor {
    private final int offsetX;
    private final int healthBarY;
    private final int hudExperienceOffset;
    private int bubblesBarY;
    private int armorBarY;
    private final RestoredHeartsDrawHelper restoredHeartsDrawHelper;
    public static boolean isDefaultArmorHudTexture;
    private final boolean hasMountHealth;
    private final int mountHealthRows;
    private final BarAnimation barAnimation;
    public boolean locatorBarWasRendered = false;

    public DrawHudContext(
            Minecraft client, Matrix3x2fStack matrices, GuiRenderState state, int i, int j, RestoredHeartsDrawHelper restoredHeartsDrawHelper,
            int hudExperienceOffset, BarAnimation barAnimation, boolean hasMountHealth, int mountHealthRows
    ) {
        super(client, matrices, state, i, j);
        this.restoredHeartsDrawHelper = restoredHeartsDrawHelper;
        var windowWidth = this.guiWidth();
        // fixing offset for odd window width value because vanilla code does integer division by 2 when
        // calculating the x coordinate
        offsetX = windowWidth - ((windowWidth % 2 == 0) ? 9 : 10);
        healthBarY = this.guiHeight() - 39 - hudExperienceOffset;
        this.hudExperienceOffset = hudExperienceOffset;
        this.hasMountHealth = hasMountHealth;
        this.mountHealthRows = mountHealthRows;
        this.barAnimation = barAnimation;
    }

    public RestoredHeartsDrawHelper getHelper() {
        return restoredHeartsDrawHelper;
    }

    public int getHudExperienceOffset() {
        return hudExperienceOffset;
    }

    public void prepareArmorAndBubblesBarsDrawing(int healthBarHighestRowY) {
        var aboveHealthY = healthBarHighestRowY - 10;
        if (hasMountHealth) {
            // drawing armor above all health rows
            armorBarY = aboveHealthY;
            // drawing bubbles above all mount health rows
            bubblesBarY = aboveHealthY - 10 * (mountHealthRows - 1);
        }
        else if (MelancholicConfig.hideHungerBar()) {
            // drawing armor in place of hunger bar (same height as health)
            armorBarY = healthBarY;
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

    public boolean getHasMountHealth() {
        return hasMountHealth;
    }

    public BarAnimation getBarAnimation() {
        return barAnimation;
    }
}
