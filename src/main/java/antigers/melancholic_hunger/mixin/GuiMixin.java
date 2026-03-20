package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.RaisedCompat;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.RestoredHeartsDrawHelper;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin extends GuiComponent implements ExperienceHudRenderer {
    @Shadow @Final protected Minecraft minecraft;
    @Shadow protected int screenWidth;
    @Shadow protected int screenHeight;
    @Shadow public void renderExperienceBar(PoseStack poseStack, int x) {}
    @Shadow public abstract Font getFont();

    @Unique private final ExperienceBarAnimation melancholic_hunger$experienceBarAnimation = new ExperienceBarAnimation();
    @Unique private DrawHudContext melancholic_hunger$drawHudContext;

    public ExperienceBarAnimation melancholic_hunger$getExperienceBarAnimation() {
        return melancholic_hunger$experienceBarAnimation;
    }

    public DrawHudContext melancholic_hunger$getDrawHudContext() {
        return melancholic_hunger$drawHudContext;
    }

    public void melancholic_hunger$setDrawHudContext(DrawHudContext drawHudContext) {
        melancholic_hunger$drawHudContext = drawHudContext;
    }

    /**
     * Changes height of both the experience bar and the experience level according to the animation position
     */
    @WrapOperation(
            method = "renderExperienceBar",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/Gui;screenHeight:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int melancholic_hunger$modifyExpBarY(Gui instance, Operation<Integer> original) {
        return original.call(instance) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
    }

    @Unique
    private void melancholic_hunger$innerBlitWithColor(
            Matrix4f pMatrix, int pX1, int pX2, int pY1, int pY2, float pMinU, float pMaxU, float pMinV, float pMaxV,
            float pAlpha
    ) {
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.enableBlend();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(pMatrix, (float)pX1, (float)pY1, 0F).color(1F, 1F, 1F, pAlpha).uv(pMinU, pMinV).endVertex();
        bufferbuilder.vertex(pMatrix, (float)pX1, (float)pY2, 0F).color(1F, 1F, 1F, pAlpha).uv(pMinU, pMaxV).endVertex();
        bufferbuilder.vertex(pMatrix, (float)pX2, (float)pY2, 0F).color(1F, 1F, 1F, pAlpha).uv(pMaxU, pMaxV).endVertex();
        bufferbuilder.vertex(pMatrix, (float)pX2, (float)pY1, 0F).color(1F, 1F, 1F, pAlpha).uv(pMaxU, pMinV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
    }

    /**
     * Sets opacity of the experience bar according to the animation
     */
    @WrapOperation(
            method = "renderExperienceBar",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
            )
    )
    private void melancholic_hunger$addExperienceBarShading(
            Gui instance, PoseStack poseStack, int x1, int y1, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
    ) {
        float u1 = uOffset / 256F, u2 = (uOffset + uWidth) / 256F;
        float v1 = vOffset / 256F, v2 = (vOffset + vHeight) / 256F;
        int x2 = x1 + uWidth;
        int y2 = y1 + vHeight;
        float currentOpacity = melancholic_hunger$experienceBarAnimation.getCurrentOpacity();
        melancholic_hunger$innerBlitWithColor(poseStack.last().pose(), x1, x2, y1, y2, u1, u2, v1, v2, currentOpacity);
    }

    /**
     * Sets opacity of the experience level according to the animation
     */
    @Inject(
            method = "renderExperienceBar",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
                    shift=At.Shift.AFTER,
                    ordinal=1
            ),
            cancellable = true
    )
    private void melancholic_hunger$addExperienceLevelShading(PoseStack pPoseStack, int pXPos, CallbackInfo ci) {
        ci.cancel();
        String s = "" + this.minecraft.player.experienceLevel;
        int i1 = (this.screenWidth - this.getFont().width(s)) / 2;
        int j1 = this.screenHeight - 28 - melancholic_hunger$experienceBarAnimation.getCurrentPos();
        int alpha = Math.max(4, (int) (melancholic_hunger$experienceBarAnimation.getCurrentOpacity() * 255));
        int zeroColor = FastColor.ARGB32.color(alpha, 0, 0, 0);
        int greenColor = FastColor.ARGB32.color(alpha, 128, 255, 32);
        this.getFont().draw(pPoseStack, s, (float)(i1 + 1), (float)j1, zeroColor);
        this.getFont().draw(pPoseStack, s, (float)(i1 - 1), (float)j1, zeroColor);
        this.getFont().draw(pPoseStack, s, (float)i1, (float)(j1 + 1), zeroColor);
        this.getFont().draw(pPoseStack, s, (float)i1, (float)(j1 - 1), zeroColor);
        this.getFont().draw(pPoseStack, s, (float)i1, (float)j1, greenColor);
        this.minecraft.getProfiler().pop();
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
        if (this.minecraft.gameMode == null || !this.minecraft.gameMode.hasExperience()) {
            return false;
        }
        var currentScreen = this.minecraft.screen;
        return (
                (MelancholicConfig.showExperienceInInventory() && currentScreen instanceof InventoryScreen) ||
                (MelancholicConfig.showExperienceOnScreens() && (
                        currentScreen instanceof FurnaceScreen || currentScreen instanceof BlastFurnaceScreen
                        || currentScreen instanceof SmokerScreen || currentScreen instanceof EnchantmentScreen
                        || currentScreen instanceof AnvilScreen || currentScreen instanceof GrindstoneScreen
                ))
        );
    }

    public void melancholic_hunger$renderExperienceHud(PoseStack poseStack) {
        if (MelancholicConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            if (InstalledMods.RAISED) {
                RaisedCompat.startHotbarTranslate(poseStack);
            }
            this.renderExperienceBar(poseStack, screenWidth / 2 - 91);
            if (InstalledMods.RAISED) {
                RaisedCompat.endTranslate(poseStack);
            }
        }
    }

    public void melancholic_hunger$onAddExperience() {
        if (MelancholicConfig.showExperienceOnGain()) {
            melancholic_hunger$experienceBarAnimation.onGainExperience();
        }
    }

    /**
     * Moves health bar down because experience bar is disabled
     */
    @WrapMethod(method="renderHearts")
    private void melancholic_hunger$moveHealthBar(
            PoseStack poseStack, Player player, int x, int y, int lines,
            int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking,
            Operation<Void> original
    ) {
        original.call(
                poseStack, player, x, melancholic_hunger$drawHudContext.getHealthBarY(), lines, regeneratingHeartIndex,
                maxHealth, lastHealth, health, absorption, blinking
        );
    }

    @Unique
    private void melancholic_hunger$drawHeartWithColor(
            PoseStack poseStack, Gui.HeartType type, int x, int y, int yOffset, boolean renderHighlight, boolean halfHeart,
            int colorRed, int colorGreen, int colorBlue
    ) {
        RenderSystem.setShaderColor(colorRed / 255F, colorGreen / 255F, colorBlue / 255F, 1.0F);
        blit(poseStack, x, y, type.getX(halfHeart, renderHighlight), yOffset, 9, 9);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws amount of hearts that can be restored by eating currently held food item
     */
    @WrapOperation(
            method="renderHearts",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderHeart(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Gui$HeartType;IIIZZ)V"
            )
    )
    private void melancholic_hunger$drawRestoredHearts(
            Gui inGameHud, PoseStack poseStack, Gui.HeartType type, int x, int y, int yOffset, boolean renderHighlight,
            boolean halfHeart, Operation<Void> original
    ) {
        RestoredHeartsDrawHelper restoredHeartsDrawHelper = melancholic_hunger$drawHudContext.getHelper();
        if (type != Gui.HeartType.CONTAINER) {
            original.call(
                    inGameHud, poseStack, type, x, restoredHeartsDrawHelper.getCurrentY(), yOffset, renderHighlight, halfHeart
            );
            return;
        }
        y = restoredHeartsDrawHelper.updateCurrentY(y);
        var res = restoredHeartsDrawHelper.heartsToDraw();
        RestoredHeartsDrawHelper.RestoredHeart firstHeart = res.getFirst();
        if (firstHeart != null) {
            // drawing container for correct background
            original.call(inGameHud, poseStack, Gui.HeartType.CONTAINER, x, y, yOffset, renderHighlight, halfHeart);
            melancholic_hunger$drawHeartWithColor(
                    poseStack, firstHeart.heartType(), x, y, yOffset, renderHighlight, firstHeart.isHalf(),
                    firstHeart.colorRed(), firstHeart.colorGreen(), firstHeart.colorBlue()
            );
            RestoredHeartsDrawHelper.RestoredHeart secondHeart = res.getSecond();
            if (secondHeart != null) {
                // drawing second heart on top of the first
                melancholic_hunger$drawHeartWithColor(
                        poseStack, secondHeart.heartType(), x, y, yOffset, renderHighlight, secondHeart.isHalf(),
                        secondHeart.colorRed(), secondHeart.colorGreen(), secondHeart.colorBlue()
                );
            }
        }
        else {
            original.call(inGameHud, poseStack, type, x, y, yOffset, renderHighlight, halfHeart);
        }
        restoredHeartsDrawHelper.updateCurrentHeart();
    }

    @WrapOperation(
            method="renderEffects",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/resources/MobEffectTextureManager;get(Lnet/minecraft/world/effect/MobEffect;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
            )
    )
    private TextureAtlasSprite melancholic_hunger$getEffectSprite(
            MobEffectTextureManager instance, MobEffect effect, Operation<TextureAtlasSprite> original
    ) {
        if (InstalledMods.FARMERS_DELIGHT) {
            effect = NourishmentEffectHandler.getEffectForSprite(effect);
        }
        return original.call(instance, effect);
    }
}