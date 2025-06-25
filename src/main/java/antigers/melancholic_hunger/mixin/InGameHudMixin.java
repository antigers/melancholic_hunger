package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.world.ClientWaypointHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements ExperienceHudRenderer {
    @Shadow private Pair<InGameHud.BarType, Bar> currentBar;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Nullable protected abstract PlayerEntity getCameraPlayer();
    @Shadow protected abstract int getHeartCount(@Nullable LivingEntity entity);
    @Shadow protected abstract int getHeartRows(int heartCount);
    @Shadow @Nullable protected abstract LivingEntity getRiddenEntity();
    @Shadow public abstract TextRenderer getTextRenderer();
    @Shadow @Final private Random random;
    @Shadow @Final private static Identifier ARMOR_EMPTY_TEXTURE;
    @Shadow @Final private static Identifier ARMOR_HALF_TEXTURE;
    @Shadow private boolean shouldShowExperienceBar() {return false;}
    @Shadow private boolean shouldShowJumpBar() {return false;}
    @Shadow private InGameHud.BarType getCurrentBarType() {return InGameHud.BarType.EMPTY;}

    @Unique private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.ofVanilla(
            "hud/experience_bar_background"
    );
    @Unique private static final Identifier EXPERIENCE_BAR_PROGRESS_TEXTURE = Identifier.ofVanilla(
            "hud/experience_bar_progress"
    );
    @Unique private static final Identifier VANILLA_ARMOR_EMPTY_TEXTURE = Identifier.of(
            "melancholic_hunger", "hud/armor_empty"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE = Identifier.of(
            "melancholic_hunger", "hud/armor_half"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE_INVERSED = Identifier.of(
            "melancholic_hunger", "hud/armor_half_inversed"
    );
    @Unique private final BarAnimation melancholic_hunger$barAnimation = new BarAnimation(
            currentBarType -> (currentBarType != InGameHud.BarType.EMPTY)
    );
    @Unique private final BarAnimation melancholic_hunger$expLevelAnimation = new BarAnimation(
            currentBarType -> (!YACLConfig.hideExperienceBar())
    );

    /**
     * Makes so that the exp bar is not drawn on the world start
     */
    @WrapMethod(method="shouldShowExperienceBar")
    private boolean melancholic_hungerShouldShowExperienceBar(Operation<Boolean> original) {
        if (this.client.player.experienceBarDisplayStartTime <= 0) {
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
                 this.shouldShowExperienceBar() ||
                // exp should be rendered while the current screen is open
                (!YACLConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen())
        );
    }

    /**
     * Sets the position and opacity for the exp level according to the animation
     */
    @Unique
    private void melancholic_hunger$renderExperienceLevel(DrawContext context, TextRenderer textRenderer, int level) {
        Text text = Text.translatable("gui.experience.level", level);
        Objects.requireNonNull(textRenderer);
        int x = (context.getScaledWindowWidth() - textRenderer.getWidth(text)) / 2;
        int y = context.getScaledWindowHeight() - 28 - melancholic_hunger$barAnimation.getCurrentPos();
        // If alpha gets to values lower than 4, it's full opacity for some reason. So capping minimum value to 4
        int alpha = Math.max(4, (int) (melancholic_hunger$expLevelAnimation.getCurrentOpacity() * 255));
        int zeroColor = ColorHelper.withAlpha(alpha, -16777216);
        context.drawText(textRenderer, text, x + 1, y, zeroColor, false);
        context.drawText(textRenderer, text, x - 1, y, zeroColor, false);
        context.drawText(textRenderer, text, x, y + 1, zeroColor, false);
        context.drawText(textRenderer, text, x, y - 1, zeroColor, false);
        context.drawText(textRenderer, text, x, y, ColorHelper.withAlpha(alpha, -8323296), false);
    }

    /**
     * Checks if the exp level should be rendered
     */
    @WrapOperation(
            method = "renderMainHud",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"
            )
    )
    private void melancholic_hunger$wrapDrawExperienceLevel(
            DrawContext context, TextRenderer textRenderer, int level, Operation<Void> original
    ) {
        if (
                !YACLConfig.hideExperienceBar() || melancholic_hunger$shouldRenderExperience()
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
    private void melancholic_hunger$renderExperienceBar(Bar bar, DrawContext context) {
        ClientPlayerEntity clientPlayerEntity = this.client.player;
        if (clientPlayerEntity.getNextLevelExperience() <= 0) {
            return;
        }
        int x = bar.getCenterX(this.client.getWindow());
        int y = context.getScaledWindowHeight() - 22 - melancholic_hunger$barAnimation.getCurrentPos();
        int alpha = (int) (melancholic_hunger$barAnimation.getCurrentOpacity() * 255);
        int color = ColorHelper.withAlpha(alpha, Colors.WHITE);
        int progressWidth = (int)(clientPlayerEntity.experienceProgress * 183.0F);
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, 182, 5, color
        );
        if (progressWidth > 0) {
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_TEXTURE, 182, 5, 0, 0,
                    x, y, progressWidth, 5, color
            );
        }
    }

    /**
     * Calls the custom method to render the exp bar
     */
    @WrapOperation(
            method = "renderMainHud",
            at = @At(
                    value="INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void melancholic_hunger$wrapRenderBar(
            Bar bar, DrawContext context, RenderTickCounter renderTickCounter, Operation<Void> original
    ) {
        if (bar instanceof ExperienceBar) {
            melancholic_hunger$renderExperienceBar(bar, context);
            return;
        }
        original.call(bar, context, renderTickCounter);
    }

    /**
     * Disables rendering of the locator bar if it is hidden in the config
     */
    @WrapOperation(
            method = "getCurrentBarType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWaypointHandler;hasWaypoint()Z"
            )
    )
    private boolean melancholic_hunger$hideLocatorBar(ClientWaypointHandler instance, Operation<Boolean> original) {
        if (YACLConfig.hideLocatorBar()) {
            return false;
        }
        return original.call(instance);
    }

    /**
     * Manipulates which bar should be rendered according to the config values
     */
    @WrapMethod(method="getCurrentBarType")
    private InGameHud.BarType melancholic_hunger$getCurrentBarType(Operation<InGameHud.BarType> original) {
        InGameHud.BarType barType = original.call();
        if (
                (barType == InGameHud.BarType.JUMPABLE_VEHICLE && this.shouldShowJumpBar())
                        || !this.client.interactionManager.hasExperienceBar()
        ) {
            return barType;
        }
        if (
                melancholic_hunger$shouldRenderExperience()
                        // exp bar disappearance animation is not finished yet
                        || melancholic_hunger$barAnimation.shouldStillDrawExperience()
        ) {
            // making the exp bar to render in our special cases
            return InGameHud.BarType.EXPERIENCE;
        } else if (barType == InGameHud.BarType.EXPERIENCE && YACLConfig.hideExperienceBar()) {
            // making the exp bar to not render when it's set to be hidden in the config
            return InGameHud.BarType.EMPTY;
        } else if (
                YACLConfig.renderExperienceOverBackground() &&
                        melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()
        ) {
            // making any bar to not render when we have an exp bar rendered on top of everything
            return InGameHud.BarType.EMPTY;
        }
        return barType;
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
        var currentScreen = this.client.currentScreen;
        return (
                (YACLConfig.showExperienceInInventory() && currentScreen instanceof InventoryScreen) ||
                (YACLConfig.showExperienceOnScreens() && (
                        currentScreen instanceof FurnaceScreen || currentScreen instanceof BlastFurnaceScreen
                        || currentScreen instanceof SmokerScreen || currentScreen instanceof EnchantmentScreen
                        || currentScreen instanceof AnvilScreen || currentScreen instanceof GrindstoneScreen
                ))
        );
    }

    /**
     * Renders the exp bar and level from an outside draw context, which is used to render it over the screens' background
     */
    public void melancholic_hunger$renderExperienceHudOverBackground(DrawContext drawContext) {
        if (!this.client.interactionManager.hasExperienceBar()) {
            return;
        }
        if (YACLConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            melancholic_hunger$renderExperienceBar(this.currentBar.getValue(), drawContext);
            if (this.client.player.experienceLevel > 0) {
                melancholic_hunger$renderExperienceLevel(drawContext, this.client.textRenderer, this.client.player.experienceLevel);
            }
        }
    }

    /**
     * Moves the mount health bar according to the exp bar animation position if there is no mount jump bar
     */
    @WrapOperation(
            method="renderMountHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"
            )
    )
    private int melancholic_hunger$moveMountHealthBar(DrawContext drawContext, Operation<Integer> original) {
        if (this.client.player.getJumpingMount() == null) {
            return original.call(drawContext) - melancholic_hunger$barAnimation.getCurrentPos() + 7;
        }
        return original.call(drawContext);
    }

    @Unique
    private DrawHudContext melancholic_hunger$getDrawHudContext(DrawContext drawContext, InGameHud.BarType currentBarType) {
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(this.getCameraPlayer(), this.random);
        boolean hasNonExperienceBar = currentBarType == InGameHud.BarType.JUMPABLE_VEHICLE || currentBarType == InGameHud.BarType.LOCATOR;
        int mountHealthHeartCount = this.getHeartCount(this.getRiddenEntity());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getHeartRows(mountHealthHeartCount);
        return new DrawHudContext(
                this.client, drawContext.state, drawRestoredHeartsHelper,
                hasNonExperienceBar ? 7 : melancholic_hunger$barAnimation.getCurrentPos(),
                melancholic_hunger$barAnimation, hasMountHealth, mountHealthRows
        );
    }

    @WrapMethod(method = "renderMainHud")
    private void melancholic_hunger$wrapRenderMainHud(
            DrawContext drawContext, RenderTickCounter tickCounter, Operation<Void> original
    ) {
        InGameHud.BarType currentBarType = this.getCurrentBarType();

        // updating the bar animations in the begging of every frame
        boolean shouldDrawExperience = this.shouldShowExperienceBar() || melancholic_hunger$needToRenderExperienceHudOnCurrentScreen();
        InGameHud.BarType animationBarType = shouldDrawExperience ? InGameHud.BarType.EXPERIENCE : currentBarType;
        melancholic_hunger$barAnimation.update(animationBarType, shouldDrawExperience);
        melancholic_hunger$expLevelAnimation.update(animationBarType, shouldDrawExperience);

        // Replaces default DrawContext object with the custom DrawHudContext object
        var drawHudContext = melancholic_hunger$getDrawHudContext(drawContext, currentBarType);
        original.call(drawHudContext, tickCounter);
    }

    /**
     * Disables hunger bar rendering or moves it down if experience bar is disabled
     */
    @WrapOperation(
        method = "renderStatusBars",
        at = @At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V"
        )
    )
    private void melancholic_hunger$disableHungerBar(
            InGameHud instance, DrawContext drawContext, PlayerEntity player, int top, int right,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (!YACLConfig.hideHungerBar()) {
            // hunger bar is drawn at the same height as health bar
            original.call(instance, drawContext, player, drawHudContext.getHealthBarY(), right);
        }
    }

    /**
     * Calculates positions of armor and bubbles bars
     */
    @WrapOperation(
        method="renderStatusBars",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;renderArmor(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"
        )
    )
    private void melancholic_hunger$wrapRenderArmor(
            DrawContext drawContext, PlayerEntity player, int i, int j, int k, int x, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        drawHudContext.prepareArmorAndBubblesBarsDrawing(i - (j - 1) * k);
        original.call(drawContext, player, i, j, k, x);
    }

    @Unique
    private static void melancholic_hunger$drawGuiTextureInversed(
            DrawContext drawContext, Identifier texture, int x1, int y1, int width, int height
    ) {
        Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(texture);
        float u1 = sprite.getMinU(), u2 = sprite.getMaxU();
        float v1 = sprite.getMinV(), v2 = sprite.getMaxV();
        int x2 = x1 + width;
        int y2 = y1 + height;
        // swapping u1 and u2 to rotate the texture along the vertical axis
        drawContext.drawTexturedQuad(sprite.getAtlasId(), x1, y1, x2, y2, u2, u1, v1, v2);
    }

    @Unique
    private static Identifier melancholic_hunger$fixVanillaArmorTexture(Identifier texture, boolean inversed) {
        if (texture == ARMOR_HALF_TEXTURE) {
            return inversed ? VANILLA_ARMOR_HALF_TEXTURE_INVERSED : VANILLA_ARMOR_HALF_TEXTURE;
        }
        if (texture == ARMOR_EMPTY_TEXTURE) {
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
            target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
        )
    )
    private static void melancholic_hunger$moveArmorBar(
            DrawContext drawContext, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        y = drawHudContext.getArmorBarY();
        if (YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
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
        method="renderStatusBars",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"
        )
    )
    private void melancholic_hunger$moveHealthBar(
            InGameHud inGameHud, DrawContext drawContext, PlayerEntity player, int x, int y, int lines,
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
            DrawContext context, InGameHud.HeartType type, int x, int y, boolean hardcore, boolean blinking,
            boolean half, int colorRed, int colorGreen, int colorBlue
    ) {
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED, type.getTexture(hardcore, half, blinking), x, y, 9, 9,
                ColorHelper.getArgb(colorRed, colorGreen, colorBlue)
        );
    }

    /**
     * Draws amount of hearts that can be restored by eating currently held food item
     */
    @WrapOperation(
        method="renderHealthBar",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;drawHeart(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/gui/hud/InGameHud$HeartType;IIZZZ)V"
        )
    )
    private void melancholic_hunger$drawRestoredHearts(
            InGameHud inGameHud, DrawContext drawContext, InGameHud.HeartType type, int x, int y, boolean hardcore,
            boolean blinking, boolean half, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        RestoredHeartsDrawHelper restoredHeartsDrawHelper = drawHudContext.getHelper();
        if (type != InGameHud.HeartType.CONTAINER) {
            original.call(
                    inGameHud, drawContext, type, x, restoredHeartsDrawHelper.getCurrentY(), hardcore, blinking, half
            );
            return;
        }
        y = restoredHeartsDrawHelper.updateCurrentY(y);
        var res = restoredHeartsDrawHelper.heartsToDraw();
        RestoredHeartsDrawHelper.RestoredHeart firstHeart = res.getLeft();
        if (firstHeart != null) {
            // drawing container for correct background
            original.call(inGameHud, drawContext, InGameHud.HeartType.CONTAINER, x, y, hardcore, blinking, half);
            melancholic_hunger$drawHeartWithColor(
                    drawContext, firstHeart.heartType(), x, y, hardcore, blinking, firstHeart.isHalf(),
                    firstHeart.colorRed(), firstHeart.colorGreen(), firstHeart.colorBlue()
            );
            RestoredHeartsDrawHelper.RestoredHeart secondHeart = res.getRight();
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
                    target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
            )
    )
    private void melancholic_hunger$moveBubblesBar(
            DrawContext drawContext, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the left and reverse render order from left to right
            x = drawHudContext.getMirroredX(x);
        }
        original.call(drawContext, pipeline, texture, x, drawHudContext.getBubblesBarY(), width, height);
    }
}