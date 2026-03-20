package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.RaisedCompat;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.hud.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Objects;

@Mixin(Gui.class)
public abstract class GuiMixin implements ExperienceHudRenderer {
    @Shadow private Pair<Gui.ContextualInfo, ContextualBarRenderer> contextualInfoBar;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable protected abstract Player getCameraPlayer();
    @Shadow protected abstract int getVehicleMaxHearts(@Nullable LivingEntity entity);
    @Shadow protected abstract int getVisibleVehicleHeartRows(int heartCount);
    @Shadow @Nullable protected abstract LivingEntity getPlayerVehicleWithHealth();
    @Shadow @Final private RandomSource random;
    @Shadow @Final private static ResourceLocation ARMOR_EMPTY_SPRITE;
    @Shadow @Final private static ResourceLocation ARMOR_HALF_SPRITE;
    @Shadow private boolean willPrioritizeExperienceInfo() {return false;}
    @Shadow private boolean willPrioritizeJumpInfo() {return false;}
    @Shadow private Gui.ContextualInfo nextContextualInfoState() {return Gui.ContextualInfo.EMPTY;}

    @Unique private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_TEXTURE = ResourceLocation.withDefaultNamespace(
            "hud/experience_bar_background"
    );
    @Unique private static final ResourceLocation EXPERIENCE_BAR_PROGRESS_TEXTURE = ResourceLocation.withDefaultNamespace(
            "hud/experience_bar_progress"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_empty"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_half"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE_INVERSED = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_half_inversed"
    );
    @Unique private final BarAnimation melancholic_hunger$barAnimation = new BarAnimation(
            currentBarType -> (currentBarType != Gui.ContextualInfo.EMPTY)
    );
    @Unique private final BarAnimation melancholic_hunger$expLevelAnimation = new BarAnimation(
            currentBarType -> (!MelancholicConfig.hideExperienceBar())
    );

    /**
     * Makes so that the exp bar is not drawn on the world start
     */
    @WrapMethod(method="willPrioritizeExperienceInfo")
    private boolean melancholic_hungerShouldShowExperienceBar(Operation<Boolean> original) {
        if (this.minecraft.player.experienceDisplayStartTick <= 0) {
            return false;
        }
        return original.call();
    }

    /**
     * Checks for special cases in which the exp bar should be rendered when it's set to be hidden in the config
     */
    @Unique
    private boolean melancholic_hunger$shouldRenderExperience() {
        return (
                // exp has been gained recently
                (MelancholicConfig.showExperienceOnGain() && this.willPrioritizeExperienceInfo()) ||
                // exp should be rendered while the current screen is open
                (!MelancholicConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen())
        );
    }

    /**
     * Sets the position and opacity for the exp level according to the animation
     */
    @Unique
    private void melancholic_hunger$renderExperienceLevel(GuiGraphics context, Font textRenderer, int level) {
        Component text = Component.translatable("gui.experience.level", level);
        Objects.requireNonNull(textRenderer);
        int x = (context.guiWidth() - textRenderer.width(text)) / 2;
        int y = context.guiHeight() - 28 - melancholic_hunger$barAnimation.getCurrentPos();
        // If alpha gets to values lower than 4, it's full opacity for some reason. So capping minimum value to 4
        int alpha = Math.max(4, (int) (melancholic_hunger$expLevelAnimation.getCurrentOpacity() * 255));
        int zeroColor = ARGB.color(alpha, -16777216);
        context.drawString(textRenderer, text, x + 1, y, zeroColor, false);
        context.drawString(textRenderer, text, x - 1, y, zeroColor, false);
        context.drawString(textRenderer, text, x, y + 1, zeroColor, false);
        context.drawString(textRenderer, text, x, y - 1, zeroColor, false);
        context.drawString(textRenderer, text, x, y, ARGB.color(alpha, -8323296), false);
    }

    /**
     * Checks if the exp level should be rendered
     */
    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"
            )
    )
    private void melancholic_hunger$wrapDrawExperienceLevel(
            GuiGraphics context, Font textRenderer, int level, Operation<Void> original
    ) {
        if (
                !MelancholicConfig.hideExperienceBar() || melancholic_hunger$shouldRenderExperience()
                        // exp lvl disappearance animation is not finished yet
                        || melancholic_hunger$expLevelAnimation.shouldStillDrawExperience()
        ) {
            melancholic_hunger$renderExperienceLevel(context, textRenderer, level);
        }
    }

    /**
     * Sets the position and opacity for the exp bar according to the animation
     */
    @Unique
    private void melancholic_hunger$renderExperienceBar(ContextualBarRenderer bar, GuiGraphics context) {
        LocalPlayer clientPlayerEntity = this.minecraft.player;
        if (clientPlayerEntity.getXpNeededForNextLevel() <= 0) {
            return;
        }
        int x = bar.left(this.minecraft.getWindow());
        int y = context.guiHeight() - 22 - melancholic_hunger$barAnimation.getCurrentPos();
        int alpha = (int) (melancholic_hunger$barAnimation.getCurrentOpacity() * 255);
        int color = ARGB.color(alpha, CommonColors.WHITE);
        int progressWidth = (int)(clientPlayerEntity.experienceProgress * 183.0F);
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, 182, 5, color
        );
        if (progressWidth > 0) {
            context.blitSprite(
                    RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_TEXTURE, 182, 5, 0, 0,
                    x, y, progressWidth, 5, color
            );
        }
    }

    /**
     * Calls the custom method to render the exp bar
     */
    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"
            )
    )
    private void melancholic_hunger$wrapRenderBar(
            ContextualBarRenderer bar, GuiGraphics context, DeltaTracker renderTickCounter, Operation<Void> original
    ) {
        if (bar instanceof ExperienceBarRenderer) {
            melancholic_hunger$renderExperienceBar(bar, context);
            return;
        }
        original.call(bar, context, renderTickCounter);
    }

    /**
     * Disables rendering of the locator bar if it is hidden in the config
     */
    @WrapOperation(
            method = "nextContextualInfoState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"
            )
    )
    private boolean melancholic_hunger$hideLocatorBar(ClientWaypointManager instance, Operation<Boolean> original) {
        if (MelancholicConfig.hideLocatorBar()) {
            return false;
        }
        return original.call(instance);
    }

    /**
     * Manipulates which bar should be rendered according to the config values
     */
    @WrapMethod(method="nextContextualInfoState")
    private Gui.ContextualInfo melancholic_hunger$getCurrentBarType(Operation<Gui.ContextualInfo> original) {
        Gui.ContextualInfo barType = original.call();
        if (
                (barType == Gui.ContextualInfo.JUMPABLE_VEHICLE && this.willPrioritizeJumpInfo())
                        || !this.minecraft.gameMode.hasExperience()
        ) {
            return barType;
        }
        if (
                melancholic_hunger$shouldRenderExperience()
                        // exp bar disappearance animation is not finished yet
                        || melancholic_hunger$barAnimation.shouldStillDrawExperience()
        ) {
            // making the exp bar to render in our special cases
            return Gui.ContextualInfo.EXPERIENCE;
        } else if (barType == Gui.ContextualInfo.EXPERIENCE && MelancholicConfig.hideExperienceBar()) {
            // making the exp bar to not render when it's set to be hidden in the config
            return Gui.ContextualInfo.EMPTY;
        } else if (
                MelancholicConfig.renderExperienceOverBackground() &&
                        melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()
        ) {
            // making any bar to not render when we have an exp bar rendered on top of everything
            return Gui.ContextualInfo.EMPTY;
        }
        return barType;
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
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

    /**
     * Renders the exp bar and level from an outside draw context, which is used to render it over the screens' background
     */
    public void melancholic_hunger$renderExperienceHudOverBackground(GuiGraphics drawContext) {
        if (!this.minecraft.gameMode.hasExperience()) {
            return;
        }
        if (MelancholicConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            if (InstalledMods.RAISED) {
                RaisedCompat.startHotbarTranslate(drawContext);
            }
            melancholic_hunger$renderExperienceBar(this.contextualInfoBar.getValue(), drawContext);
            if (this.minecraft.player.experienceLevel > 0) {
                melancholic_hunger$renderExperienceLevel(drawContext, this.minecraft.font, this.minecraft.player.experienceLevel);
            }
            if (InstalledMods.RAISED) {
                RaisedCompat.endTranslate(drawContext);
            }
        }
    }

    /**
     * Moves the mount health bar according to the exp bar animation position if there is no mount jump bar
     */
    @WrapOperation(
            method="renderVehicleHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"
            )
    )
    private int melancholic_hunger$moveMountHealthBar(GuiGraphics drawContext, Operation<Integer> original) {
        if (this.minecraft.player.jumpableVehicle() == null) {
            return original.call(drawContext) - melancholic_hunger$barAnimation.getCurrentPos() + 7;
        }
        return original.call(drawContext);
    }

    @Unique
    private DrawHudContext melancholic_hunger$getDrawHudContext(GuiGraphics drawContext, Gui.ContextualInfo currentBarType) {
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(this.getCameraPlayer(), this.random);
        boolean hasNonExperienceBar = currentBarType == Gui.ContextualInfo.JUMPABLE_VEHICLE || currentBarType == Gui.ContextualInfo.LOCATOR;
        int mountHealthHeartCount = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getVisibleVehicleHeartRows(mountHealthHeartCount);
        return new DrawHudContext(
                this.minecraft, drawContext.pose(), drawContext.guiRenderState, drawRestoredHeartsHelper,
                hasNonExperienceBar ? 7 : melancholic_hunger$barAnimation.getCurrentPos(),
                melancholic_hunger$barAnimation, hasMountHealth, mountHealthRows
        );
    }

    @WrapMethod(method = "renderHotbarAndDecorations")
    private void melancholic_hunger$wrapRenderMainHud(
            GuiGraphics drawContext, DeltaTracker tickCounter, Operation<Void> original
    ) {
        Gui.ContextualInfo currentBarType = this.nextContextualInfoState();

        // updating the bar animations in the begging of every frame
        boolean shouldDrawExperience = (MelancholicConfig.showExperienceOnGain() && this.willPrioritizeExperienceInfo()) ||
                melancholic_hunger$needToRenderExperienceHudOnCurrentScreen();
        Gui.ContextualInfo animationBarType = shouldDrawExperience ? Gui.ContextualInfo.EXPERIENCE : currentBarType;
        melancholic_hunger$barAnimation.update(animationBarType, shouldDrawExperience);
        melancholic_hunger$expLevelAnimation.update(animationBarType, shouldDrawExperience);

        // Replaces default GuiGraphics object with the custom DrawHudContext object
        var drawHudContext = melancholic_hunger$getDrawHudContext(drawContext, currentBarType);
        original.call(drawHudContext, tickCounter);
    }

    /**
     * Disables hunger bar rendering or moves it down if experience bar is disabled
     */
    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderFood(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;II)V"
            )
    )
    private void melancholic_hunger$disableHungerBar(
            Gui instance, GuiGraphics drawContext, Player player, int top, int right,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (!MelancholicConfig.hideHungerBar()) {
            // hunger bar is drawn at the same height as health bar
            original.call(instance, drawContext, player, drawHudContext.getHealthBarY(), right);
        }
    }

    /**
     * Calculates positions of armor and bubbles bars
     */
    @WrapOperation(
            method="renderPlayerHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V"
            )
    )
    private void melancholic_hunger$wrapRenderArmor(
            GuiGraphics drawContext, Player player, int i, int j, int k, int x, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        drawHudContext.prepareArmorAndBubblesBarsDrawing(i - (j - 1) * k);
        original.call(drawContext, player, i, j, k, x);
    }

    @Unique
    private static void melancholic_hunger$drawGuiTextureInversed(
            GuiGraphics drawContext, ResourceLocation texture, int x1, int y1, int width, int height
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        float u1 = sprite.getU0(), u2 = sprite.getU1();
        float v1 = sprite.getV0(), v2 = sprite.getV1();
        int x2 = x1 + width;
        int y2 = y1 + height;
        // swapping u1 and u2 to rotate the texture along the vertical axis
        drawContext.blit(sprite.atlasLocation(), x1, y1, x2, y2, u2, u1, v1, v2);
    }

    @Unique
    private static ResourceLocation melancholic_hunger$fixVanillaArmorTexture(ResourceLocation texture, boolean inversed) {
        if (texture == ARMOR_HALF_SPRITE) {
            return inversed ? VANILLA_ARMOR_HALF_TEXTURE_INVERSED : VANILLA_ARMOR_HALF_TEXTURE;
        }
        if (texture == ARMOR_EMPTY_SPRITE) {
            return VANILLA_ARMOR_EMPTY_TEXTURE;
        }
        return texture;
    }

    /**
     * Moves armor bar right and down because hunger and experience bars are disabled
     */
    @WrapOperation(
            method="renderArmor",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private static void melancholic_hunger$moveArmorBar(
            GuiGraphics drawContext, RenderPipeline pipeline, ResourceLocation texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        y = drawHudContext.getArmorBarY();
        if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the right and reverse render order from right to left
            x = drawHudContext.getMirroredX(x);
            if (!DrawHudContext.isDefaultArmorHudTexture) {
                melancholic_hunger$drawGuiTextureInversed(drawContext, texture, x, y, width, height);
                return;
            }
            texture = melancholic_hunger$fixVanillaArmorTexture(texture, true);
        }
        else if (DrawHudContext.isDefaultArmorHudTexture) {
            texture = melancholic_hunger$fixVanillaArmorTexture(texture, false);
        }
        original.call(drawContext, pipeline, texture, x, y, width, height);
    }

    /**
     * Moves health bar down because experience bar is disabled
     */
    @WrapOperation(
            method="renderPlayerHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"
            )
    )
    private void melancholic_hunger$moveHealthBar(
            Gui inGameHud, GuiGraphics drawContext, Player player, int x, int y, int lines,
            int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        original.call(
                inGameHud, drawContext, player, x, drawHudContext.getHealthBarY(), lines, regeneratingHeartIndex,
                maxHealth, lastHealth, health, absorption, blinking
        );
    }

    @Unique
    private void melancholic_hunger$drawHeartWithColor(
            GuiGraphics context, Gui.HeartType type, int x, int y, boolean hardcore, boolean blinking,
            boolean half, int colorRed, int colorGreen, int colorBlue
    ) {
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED, type.getSprite(hardcore, half, blinking), x, y, 9, 9,
                ARGB.color(colorRed, colorGreen, colorBlue)
        );
    }

    /**
     * Draws amount of hearts that can be restored by eating currently held food item
     */
    @WrapOperation(
            method="renderHearts",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V"
            )
    )
    private void melancholic_hunger$drawRestoredHearts(
            Gui inGameHud, GuiGraphics drawContext, Gui.HeartType type, int x, int y, boolean hardcore,
            boolean blinking, boolean half, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        RestoredHeartsDrawHelper restoredHeartsDrawHelper = drawHudContext.getHelper();
        if (type != Gui.HeartType.CONTAINER) {
            original.call(
                    inGameHud, drawContext, type, x, restoredHeartsDrawHelper.getCurrentY(), hardcore, blinking, half
            );
            return;
        }
        y = restoredHeartsDrawHelper.updateCurrentY(y);
        var res = restoredHeartsDrawHelper.heartsToDraw();
        RestoredHeartsDrawHelper.RestoredHeart firstHeart = res.getFirst();
        if (firstHeart != null) {
            // drawing container for correct background
            original.call(inGameHud, drawContext, Gui.HeartType.CONTAINER, x, y, hardcore, blinking, half);
            melancholic_hunger$drawHeartWithColor(
                    drawContext, firstHeart.heartType(), x, y, hardcore, blinking, firstHeart.isHalf(),
                    firstHeart.colorRed(), firstHeart.colorGreen(), firstHeart.colorBlue()
            );
            RestoredHeartsDrawHelper.RestoredHeart secondHeart = res.getSecond();
            if (secondHeart != null) {
                // drawing second heart on top of the first
                melancholic_hunger$drawHeartWithColor(
                        drawContext, secondHeart.heartType(), x, y, hardcore, blinking, secondHeart.isHalf(),
                        secondHeart.colorRed(), secondHeart.colorGreen(), secondHeart.colorBlue()
                );
            }
        }
        else {
            original.call(inGameHud, drawContext, type, x, y, hardcore, blinking, half);
        }
        restoredHeartsDrawHelper.updateCurrentHeart();
    }

    /**
     * Move air bubbles on top of health rows
     */
    @WrapOperation(
            method="renderAirBubbles",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void melancholic_hunger$moveBubblesBar(
            GuiGraphics drawContext, RenderPipeline pipeline, ResourceLocation texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the left and reverse render order from left to right
            x = drawHudContext.getMirroredX(x);
        }
        original.call(drawContext, pipeline, texture, x, drawHudContext.getBubblesBarY(), width, height);
    }

    @WrapMethod(method="getMobEffectSprite")
    private static ResourceLocation melancholic_hunger$getEffectSprite(Holder<MobEffect> effect, Operation<ResourceLocation> original) {
        if (InstalledMods.FARMERS_DELIGHT) {
            effect = NourishmentEffectHandler.getEffectForSprite(effect);
        }
        return original.call(effect);
    }
}