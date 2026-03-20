package antigers.melancholic_hunger.hud.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.MelancholicHunger;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
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
    @Shadow @Final private static Identifier ARMOR_EMPTY_SPRITE;
    @Shadow @Final private static Identifier ARMOR_HALF_SPRITE;
    @Shadow private boolean willPrioritizeExperienceInfo() {return false;}
    @Shadow private boolean willPrioritizeJumpInfo() {return false;}
    @Shadow private Gui.ContextualInfo nextContextualInfoState() {return Gui.ContextualInfo.EMPTY;}
    @Shadow public int leftHeight;
    @Shadow public int rightHeight;

    @Unique private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace(
            "hud/experience_bar_background"
    );
    @Unique private static final Identifier EXPERIENCE_BAR_PROGRESS_TEXTURE = Identifier.withDefaultNamespace(
            "hud/experience_bar_progress"
    );
    @Unique private static final Identifier VANILLA_ARMOR_EMPTY_TEXTURE = Identifier.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "hud/armor_empty"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE = Identifier.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "hud/armor_half"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE_INVERSED = Identifier.fromNamespaceAndPath(
            MelancholicHunger.MOD_ID, "hud/armor_half_inversed"
    );
    @Unique private final BarAnimation melancholic_hunger$barAnimation = new BarAnimation(
            currentBarType -> (currentBarType != Gui.ContextualInfo.EMPTY)
    );
    @Unique private final BarAnimation melancholic_hunger$expLevelAnimation = new BarAnimation(
            _ -> (!MelancholicConfig.hideExperienceBar())
    );

    /**
     * Makes so that the exp bar is not drawn on the world start
     */
    @WrapMethod(method="willPrioritizeExperienceInfo")
    private boolean melancholic_hunger$shouldShowExperienceBar(Operation<Boolean> original) {
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
    private void melancholic_hunger$renderExperienceLevel(GuiGraphicsExtractor graphics, Font textRenderer, int level) {
        Component text = Component.translatable("gui.experience.level", level);
        Objects.requireNonNull(textRenderer);
        int x = (graphics.guiWidth() - textRenderer.width(text)) / 2;
        int y = graphics.guiHeight() - 28 - melancholic_hunger$barAnimation.getCurrentPos();
        // If alpha gets to values lower than 4, it's full opacity for some reason. So capping minimum value to 4
        int alpha = Math.max(4, (int) (melancholic_hunger$expLevelAnimation.getCurrentOpacity() * 255));
        int zeroColor = ARGB.color(alpha, -16777216);
        graphics.text(textRenderer, text, x + 1, y, zeroColor, false);
        graphics.text(textRenderer, text, x - 1, y, zeroColor, false);
        graphics.text(textRenderer, text, x, y + 1, zeroColor, false);
        graphics.text(textRenderer, text, x, y - 1, zeroColor, false);
        graphics.text(textRenderer, text, x, y, ARGB.color(alpha, -8323296), false);
    }

    /**
     * Checks if the exp level should be rendered
     */
    @WrapOperation(
            method = "extractExperienceLevel",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"
            )
    )
    private void melancholic_hunger$wrapDrawExperienceLevel(
            GuiGraphicsExtractor graphics, Font textRenderer, int level, Operation<Void> original
    ) {
        if (
                !MelancholicConfig.hideExperienceBar() || melancholic_hunger$shouldRenderExperience()
                        // exp lvl disappearance animation is not finished yet
                        || melancholic_hunger$expLevelAnimation.shouldStillDrawExperience()
        ) {
            melancholic_hunger$renderExperienceLevel(graphics, textRenderer, level);
        }
    }

    /**
     * Sets the position and opacity for the exp bar according to the animation
     */
    @Unique
    private void melancholic_hunger$renderExperienceBar(ContextualBarRenderer bar, GuiGraphicsExtractor graphics) {
        LocalPlayer clientPlayerEntity = this.minecraft.player;
        if (clientPlayerEntity.getXpNeededForNextLevel() <= 0) {
            return;
        }
        int x = bar.left(this.minecraft.getWindow());
        int y = graphics.guiHeight() - 22 - melancholic_hunger$barAnimation.getCurrentPos();
        int alpha = (int) (melancholic_hunger$barAnimation.getCurrentOpacity() * 255);
        int color = ARGB.color(alpha, CommonColors.WHITE);
        int progressWidth = (int)(clientPlayerEntity.experienceProgress * 183.0F);
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, 182, 5, color
        );
        if (progressWidth > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_TEXTURE, 182, 5, 0, 0,
                    x, y, progressWidth, 5, color
            );
        }
    }

    /**
     * Calls the custom method to render the exp bar
     */
    @WrapOperation(
            method = "extractContextualInfoBarBackground",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            )
    )
    private void melancholic_hunger$wrapRenderBar(
            ContextualBarRenderer bar, GuiGraphicsExtractor graphics, DeltaTracker renderTickCounter, Operation<Void> original
    ) {
        if (bar instanceof ExperienceBarRenderer) {
            melancholic_hunger$renderExperienceBar(bar, graphics);
            return;
        }
        original.call(bar, graphics, renderTickCounter);
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
     * Renders the exp bar and level from an outside draw graphics, which is used to render it over the screens' background
     */
    public void melancholic_hunger$renderExperienceHudOverBackground(GuiGraphicsExtractor graphics) {
        if (!this.minecraft.gameMode.hasExperience()) {
            return;
        }
        if (MelancholicConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            if (InstalledMods.RAISED) {
                RaisedCompat.startHotbarTranslate(graphics);
            }
            melancholic_hunger$renderExperienceBar(this.contextualInfoBar.getValue(), graphics);
            if (this.minecraft.player.experienceLevel > 0) {
                melancholic_hunger$renderExperienceLevel(graphics, this.minecraft.font, this.minecraft.player.experienceLevel);
            }
            if (InstalledMods.RAISED) {
                RaisedCompat.endTranslate(graphics);
            }
        }
    }

    /**
     * Moves the mount health bar according to the exp bar animation position if there is no mount jump bar
     */
    @WrapOperation(
            method="extractVehicleHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"
            )
    )
    private int melancholic_hunger$moveMountHealthBar(GuiGraphicsExtractor graphics, Operation<Integer> original) {
        if (this.minecraft.player.jumpableVehicle() == null) {
            return original.call(graphics) - melancholic_hunger$barAnimation.getCurrentPos() + 7;
        }
        return original.call(graphics);
    }

    @Unique
    private DrawHudContext melancholic_hunger$getDrawHudContext(GuiGraphicsExtractor graphics, Gui.ContextualInfo currentBarType) {
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(this.getCameraPlayer(), this.random);
        boolean hasNonExperienceBar = currentBarType == Gui.ContextualInfo.JUMPABLE_VEHICLE || currentBarType == Gui.ContextualInfo.LOCATOR;
        int mountHealthHeartCount = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getVisibleVehicleHeartRows(mountHealthHeartCount);
        return new DrawHudContext(
                this.minecraft, graphics.pose(), graphics.guiRenderState, graphics.mouseX, graphics.mouseY,
                drawRestoredHeartsHelper, hasNonExperienceBar ? 7 : melancholic_hunger$barAnimation.getCurrentPos(),
                melancholic_hunger$barAnimation, hasMountHealth, mountHealthRows
        );
    }

    @WrapOperation(
            method="extractRenderState",
            at=@At(
                    value="INVOKE",
                    target="Lnet/neoforged/neoforge/client/gui/GuiLayerManager;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            )
    )
    private void melancholic_hunger$wrapRenderMainHud(
            GuiLayerManager guiLayerManager, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original
    ) {
        Gui.ContextualInfo currentBarType = this.nextContextualInfoState();

        // updating the bar animations in the begging of every frame
        boolean shouldDrawExperience = (MelancholicConfig.showExperienceOnGain() && this.willPrioritizeExperienceInfo()) ||
                melancholic_hunger$needToRenderExperienceHudOnCurrentScreen();
        Gui.ContextualInfo animationBarType = shouldDrawExperience ? Gui.ContextualInfo.EXPERIENCE : currentBarType;
        melancholic_hunger$barAnimation.update(animationBarType, shouldDrawExperience);
        melancholic_hunger$expLevelAnimation.update(animationBarType, shouldDrawExperience);

        // Replaces default GuiGraphicsExtractor object with the custom DrawHudContext object
        var drawHudContext = melancholic_hunger$getDrawHudContext(graphics, currentBarType);

        // setting up neoforge gui heights in accordance with experience bar offset
        int experienceOffset = drawHudContext.getHudExperienceOffset() - 7;
        leftHeight += experienceOffset;
        rightHeight += experienceOffset;

        original.call(guiLayerManager, drawHudContext, deltaTracker);
    }

    /**
     * Disables hunger bar rendering or moves it down if experience bar is disabled
     */
    @WrapMethod(method = "extractFood")
    private void melancholic_hunger$disableHungerBar(
            GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        if (!MelancholicConfig.hideHungerBar()) {
            // hunger bar is drawn at the same height as health bar
            original.call(graphics, player, drawHudContext.getHealthBarY(), xRight);
        }
    }

    /**
     * Calculates positions of armor and bubbles bars
     */
    @WrapMethod(method="extractArmor")
    private static void melancholic_hunger$wrapRenderArmor(
            GuiGraphicsExtractor graphics, Player player, int yLineBase, int numHealthRows, int healthRowHeight, int xLeft, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        drawHudContext.prepareArmorAndBubblesBarsDrawing(yLineBase - (numHealthRows - 1) * healthRowHeight);
        original.call(graphics, player, yLineBase, numHealthRows, healthRowHeight, xLeft);
    }

    @Unique
    private static void melancholic_hunger$drawGuiTextureInversed(
            GuiGraphicsExtractor graphics, Identifier texture, int x1, int y1, int width, int height
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(texture);
        float u1 = sprite.getU0(), u2 = sprite.getU1();
        float v1 = sprite.getV0(), v2 = sprite.getV1();
        int x2 = x1 + width;
        int y2 = y1 + height;
        // swapping u1 and u2 to rotate the texture along the vertical axis
        graphics.blit(sprite.atlasLocation(), x1, y1, x2, y2, u2, u1, v1, v2);
    }

    @Unique
    private static Identifier melancholic_hunger$fixVanillaArmorTexture(Identifier texture, boolean inversed) {
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
            method="extractArmor",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private static void melancholic_hunger$moveArmorBar(
            GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        y = drawHudContext.getArmorBarY();
        if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the right and reverse render order from right to left
            x = drawHudContext.getMirroredX(x);
            if (!DrawHudContext.isDefaultArmorHudTexture) {
                melancholic_hunger$drawGuiTextureInversed(graphics, texture, x, y, width, height);
                return;
            }
            texture = melancholic_hunger$fixVanillaArmorTexture(texture, true);
        }
        else if (DrawHudContext.isDefaultArmorHudTexture) {
            texture = melancholic_hunger$fixVanillaArmorTexture(texture, false);
        }
        original.call(graphics, pipeline, texture, x, y, width, height);
    }

    /**
     * Moves health bar down because experience bar is disabled
     */
    @WrapMethod(method="extractHearts")
    private void melancholic_hunger$moveHealthBar(
            GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase, int healthRowHeight, int heartOffsetIndex,
            float maxHealth, int currentHealth, int oldHealth, int absorption, boolean blink, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        original.call(
                graphics, player, xLeft, drawHudContext.getHealthBarY(), healthRowHeight, heartOffsetIndex, maxHealth,
                currentHealth, oldHealth, absorption, blink
        );
    }

    @Unique
    private void melancholic_hunger$drawHeartWithColor(
            GuiGraphicsExtractor graphics, Gui.HeartType type, int x, int y, boolean hardcore, boolean blinking,
            boolean half, int colorRed, int colorGreen, int colorBlue
    ) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, type.getSprite(hardcore, half, blinking), x, y, 9, 9,
                ARGB.color(colorRed, colorGreen, colorBlue)
        );
    }

    /**
     * Draws amount of hearts that can be restored by eating currently held food item
     */
    @WrapOperation(
            method="extractHearts",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V"
            )
    )
    private void melancholic_hunger$drawRestoredHearts(
            Gui gui, GuiGraphicsExtractor graphics, Gui.HeartType type, int x, int y, boolean hardcore,
            boolean blinking, boolean half, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        RestoredHeartsDrawHelper restoredHeartsDrawHelper = drawHudContext.getHelper();
        if (type != Gui.HeartType.CONTAINER) {
            original.call(gui, graphics, type, x, restoredHeartsDrawHelper.getCurrentY(), hardcore, blinking, half);
            return;
        }
        y = restoredHeartsDrawHelper.updateCurrentY(y);
        var res = restoredHeartsDrawHelper.heartsToDraw();
        RestoredHeartsDrawHelper.RestoredHeart firstHeart = res.getFirst();
        if (firstHeart != null) {
            // drawing container for correct background
            original.call(gui, graphics, Gui.HeartType.CONTAINER, x, y, hardcore, blinking, half);
            melancholic_hunger$drawHeartWithColor(
                    graphics, firstHeart.heartType(), x, y, hardcore, blinking, firstHeart.isHalf(),
                    firstHeart.colorRed(), firstHeart.colorGreen(), firstHeart.colorBlue()
            );
            RestoredHeartsDrawHelper.RestoredHeart secondHeart = res.getSecond();
            if (secondHeart != null) {
                // drawing second heart on top of the first
                melancholic_hunger$drawHeartWithColor(
                        graphics, secondHeart.heartType(), x, y, hardcore, blinking, secondHeart.isHalf(),
                        secondHeart.colorRed(), secondHeart.colorGreen(), secondHeart.colorBlue()
                );
            }
        }
        else {
            original.call(gui, graphics, type, x, y, hardcore, blinking, half);
        }
        restoredHeartsDrawHelper.updateCurrentHeart();
    }

    /**
     * Move air bubbles on top of health rows
     */
    @WrapOperation(
            method="extractAirBubbles",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void melancholic_hunger$moveBubblesBar(
            GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) graphics;
        if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the left and reverse render order from left to right
            x = drawHudContext.getMirroredX(x);
        }
        original.call(graphics, pipeline, texture, x, drawHudContext.getBubblesBarY(), width, height);
    }

    @WrapMethod(method="getMobEffectSprite")
    private static Identifier melancholic_hunger$getEffectSprite(Holder<MobEffect> effect, Operation<Identifier> original) {
        if (InstalledMods.FARMERS_DELIGHT) {
            effect = NourishmentEffectHandler.getEffectForSprite(effect);
        }
        return original.call(effect);
    }
}