package antigers.melancholic_hunger.hud;

import com.mojang.blaze3d.vertex.PoseStack;

public interface ExperienceHudRenderer {
    void melancholic_hunger$renderExperienceHud(PoseStack poseStack);
    void melancholic_hunger$onAddExperience();
    ExperienceBarAnimation melancholic_hunger$getExperienceBarAnimation();
    boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen();
    DrawHudContext melancholic_hunger$getDrawHudContext();
    void melancholic_hunger$setDrawHudContext(DrawHudContext drawHudContext);
}
