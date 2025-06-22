package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement {
    @Shadow protected MinecraftClient client;

    @Inject(
        method="renderInGameBackground",
        at=@At("TAIL")
    )
    public void melancholic_hunger$renderExperienceOnTopOfBackground(DrawContext drawContext, CallbackInfo callback) {
        ExperienceHudRenderer inGameHud = (ExperienceHudRenderer) this.client.inGameHud;
        inGameHud.melancholic_hunger$renderExperienceHudOverBackground(drawContext);
    }
}
