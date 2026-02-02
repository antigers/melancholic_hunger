package antigers.melancholic_hunger.hud;

import net.minecraft.client.gui.GuiGraphics;

public interface ExperienceHudRenderer {
    void melancholic_hunger$renderExperienceHud(GuiGraphics drawContext);
    void melancholic_hunger$onAddExperience();
    ExperienceBarAnimation melancholic_hunger$getExperienceBarAnimation();
    boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen();
}
