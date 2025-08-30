package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow protected Minecraft minecraft;

    @Inject(
        method="renderTransparentBackground",
        at=@At("TAIL")
    )
    public void melancholic_hunger$renderExperienceOnTopOfBackground(GuiGraphics drawContext, CallbackInfo callback) {
        ExperienceHudRenderer inGameHud = (ExperienceHudRenderer) this.minecraft.gui;
        inGameHud.melancholic_hunger$renderExperienceHudOverBackground(drawContext);
    }
}
