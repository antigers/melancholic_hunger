package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow protected Minecraft minecraft;

    @Inject(
            method="renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at=@At("TAIL")
    )
    public void melancholic_hunger$renderExperienceOnTopOfBackground(PoseStack poseStack, CallbackInfo callback) {
        if ((Screen)(Object)this instanceof LoadingErrorScreen) {
            return;
        }
        ExperienceHudRenderer inGameHud = (ExperienceHudRenderer) this.minecraft.gui;
        inGameHud.melancholic_hunger$renderExperienceHud(poseStack);
    }
}
