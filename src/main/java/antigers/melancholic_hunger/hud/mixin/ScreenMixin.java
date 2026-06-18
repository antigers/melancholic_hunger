package antigers.melancholic_hunger.hud.mixin;

import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow
    protected Minecraft minecraft;

    @Inject(
            method = "extractTransparentBackground",
            at = @At("TAIL")
    )
    public void melancholic_hunger$renderExperienceOnTopOfBackground(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        ExperienceHudRenderer gui = (ExperienceHudRenderer) this.minecraft.gui.hud;
        gui.melancholic_hunger$renderExperienceHudOverBackground(graphics);
    }
}
