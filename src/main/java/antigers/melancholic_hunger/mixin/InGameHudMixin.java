package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.LayeredDrawer;
import net.minecraft.client.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.RestoredHeartsDrawHelper;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements ExperienceHudRenderer {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Nullable protected abstract PlayerEntity getCameraPlayer();
    @Shadow private void renderExperienceBar(DrawContext context, int x) {}
    @Shadow private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter) {}
    @Shadow protected abstract boolean shouldRenderExperience();
    @Shadow protected abstract int getHeartCount(@Nullable LivingEntity entity);
    @Shadow protected abstract int getHeartRows(int heartCount);
    @Shadow @Nullable protected abstract LivingEntity getRiddenEntity();
    @Shadow public abstract TextRenderer getTextRenderer();
    @Shadow @Final private Random random;
    @Shadow @Final private static Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE;
    @Shadow @Final private static Identifier EXPERIENCE_BAR_PROGRESS_TEXTURE;
    @Shadow @Final private static Identifier ARMOR_EMPTY_TEXTURE;
    @Shadow @Final private static Identifier ARMOR_HALF_TEXTURE;

    @Unique private static final Identifier VANILLA_ARMOR_EMPTY_TEXTURE = Identifier.of(
            "melancholic_hunger", "hud/armor_empty"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE = Identifier.of(
            "melancholic_hunger", "hud/armor_half"
    );
    @Unique private static final Identifier VANILLA_ARMOR_HALF_TEXTURE_INVERSED = Identifier.of(
            "melancholic_hunger", "hud/armor_half_inversed"
    );
    @Unique private final ExperienceBarAnimation melancholic_hunger$experienceBarAnimation = new ExperienceBarAnimation();

    @Unique
    private boolean melancholic_hunger$shouldRenderExperience() {
        if (!this.shouldRenderExperience()) {
            return false;
        }
        if (!YACLConfig.hideExperienceBar()) {
            return true;
        }
        return (
                (!YACLConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) ||
                        melancholic_hunger$experienceBarAnimation.shouldDraw()
        );
    }

    /**
     * Disables rendering of experience bar
     */
    @WrapOperation(
        method = "renderMainHud",
        at = @At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;shouldRenderExperience()Z"
        )
    )
    private boolean melancholic_hunger$disableExperienceBarRender(InGameHud instance, Operation<Boolean> original) {
        return melancholic_hunger$shouldRenderExperience();
    }

    /**
     * Disables rendering of experience level.
     * Using ordinal = 3 because renderExperienceLevel is the forth layer added to the layeredDrawer
     */
    @WrapOperation(
        method = "<init>",
        at = @At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/LayeredDrawer;addLayer(Lnet/minecraft/client/gui/LayeredDrawer$Layer;)Lnet/minecraft/client/gui/LayeredDrawer;",
            ordinal=3
        )
    )
    private LayeredDrawer melancholic_hunger$disableExperienceLevelRender(
            LayeredDrawer instance, LayeredDrawer.Layer layer, Operation<LayeredDrawer> original
    ) {
        LayeredDrawer.Layer wrappedLayer = (context, tickCounter) -> {
            if (melancholic_hunger$shouldRenderExperience()) {
                this.renderExperienceLevel(context, tickCounter);
            }
        };
        return original.call(instance, wrappedLayer);
    }

    /**
     * Sets opacity of the experience level according to the animation
     */
    @WrapMethod(
            method = "renderExperienceLevel"
    )
    private void melancholic_hunger$renderExperienceLevel(
            DrawContext context, RenderTickCounter tickCounter, Operation<Void> original
    ) {
        int level = this.client.player.experienceLevel;
        if (level <= 0) {
            return;
        }
        Profilers.get().push("expLevel");
        String string = "" + level;
        int x = (context.getScaledWindowWidth() - this.getTextRenderer().getWidth(string)) / 2;
        int y = context.getScaledWindowHeight() - 28 - melancholic_hunger$experienceBarAnimation.getCurrentPos();
        // If alpha gets to values lower than 4, it's full opacity for some reason. So capping minimum value to 4
        int alpha = Math.max(4, (int) (melancholic_hunger$experienceBarAnimation.getCurrentOpacity() * 255));
        int zeroColor = ColorHelper.withAlpha(alpha, 0);
        context.drawText(this.getTextRenderer(), string, x + 1, y, zeroColor, false);
        context.drawText(this.getTextRenderer(), string, x - 1, y, zeroColor, false);
        context.drawText(this.getTextRenderer(), string, x, y + 1, zeroColor, false);
        context.drawText(this.getTextRenderer(), string, x, y - 1, zeroColor, false);
        context.drawText(this.getTextRenderer(), string, x, y, ColorHelper.withAlpha(alpha, 8453920), false);
        Profilers.get().pop();
    }

    /**
     * Changes height of the experience level according to the animation position
     */
    @WrapOperation(
            method = "renderExperienceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"
            )
    )
    private int melancholic_hunger$modifyExpLevelY(DrawContext instance, Operation<Integer> original) {
        return original.call(instance) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
    }

    /**
     * Moves mount health bar according to the exp bar animation position if there is no mount jump bar
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
            return original.call(drawContext) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
        }
        return original.call(drawContext);
    }

    /**
     * Sets opacity of the experience bar according to the animation
     */
    @WrapMethod(
            method = "renderExperienceBar"
    )
    private void melancholic_hunger$addExperienceBarShading(DrawContext context, int x, Operation<Void> original) {
        Profilers.get().push("expBar");
        if (this.client.player.getNextLevelExperience() > 0) {
            int width = (int)(this.client.player.experienceProgress * 183.0F);
            int y = context.getScaledWindowHeight() - 22 - melancholic_hunger$experienceBarAnimation.getCurrentPos();
            int alpha = (int) (melancholic_hunger$experienceBarAnimation.getCurrentOpacity() * 255);
            int color = ColorHelper.withAlpha(alpha, Colors.WHITE);
            context.drawGuiTexture(RenderLayer::getGuiTextured, EXPERIENCE_BAR_BACKGROUND_TEXTURE, x, y, 182, 5, color);
            if (width > 0) {
                Sprite sprite = this.client.getGuiAtlasManager().getSprite(EXPERIENCE_BAR_PROGRESS_TEXTURE);
                context.drawSpriteRegion(RenderLayer::getGuiTextured, sprite, 182, 5, 0, 0, x, y, width, 5, color);
            }
        }
        Profilers.get().pop();
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
        if (!this.shouldRenderExperience()) {
            return false;
        }
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

    public void melancholic_hunger$renderExperienceHud(DrawContext drawContext) {
        if (YACLConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            this.renderExperienceBar(drawContext, drawContext.getScaledWindowWidth() / 2 - 91);
            this.renderExperienceLevel(drawContext, null);
        }
    }

    public void melancholic_hunger$onAddExperience() {
        if (YACLConfig.showExperienceOnGain()) {
            melancholic_hunger$experienceBarAnimation.onGainExperience();
        }
    }

    @Inject(method = "renderMainHud", at = @At("HEAD"))
    private void melancholic_hunger$calculateHudOffset(
            DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci
    ) {
        melancholic_hunger$experienceBarAnimation.update(melancholic_hunger$needToRenderExperienceHudOnCurrentScreen());
    }

    /**
     * Replaces default DrawContext object with the custom DrawHudContext object
     */
    @WrapOperation(
        method="renderMainHud",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/hud/InGameHud;renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V"
        )
    )
    private void melancholic_hunger$replaceDrawContext(InGameHud inGameHud, DrawContext drawContext, Operation<Void> original) {
        PlayerEntity playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(playerEntity, this.random);
        int mountHealthHeartCount = this.getHeartCount(this.getRiddenEntity());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getHeartRows(mountHealthHeartCount);
        var drawHudContext = new DrawHudContext(
                this.client, drawContext.vertexConsumers, drawRestoredHeartsHelper,
                this.client.player.getJumpingMount() != null
                        ? 7 : melancholic_hunger$experienceBarAnimation.getCurrentPos(), hasMountHealth, mountHealthRows
        );
        original.call(inGameHud, drawHudContext);
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
            DrawContext drawContext, Identifier texture, int x, int y, int width, int height
    ) {
        Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(texture);
        float u1 = sprite.getMinU(), u2 = sprite.getMaxU();
        float v1 = sprite.getMinV(), v2 = sprite.getMaxV();
        float x1 = (float) x, x2 = x + width;
        float y1 = (float) y, y2 = y + height;

        RenderLayer renderLayer = RenderLayer.getGuiTextured(sprite.getAtlasId());
        Matrix4f matrix4f = drawContext.getMatrices().peek().getPositionMatrix();
        VertexConsumer vertexConsumer = drawContext.vertexConsumers.getBuffer(renderLayer);
        vertexConsumer.vertex(matrix4f, x1, y2, 0.0F).texture(u2, v2).color(-1);
        vertexConsumer.vertex(matrix4f, x2, y2, 0.0F).texture(u1, v2).color(-1);
        vertexConsumer.vertex(matrix4f, x2, y1, 0.0F).texture(u1, v1).color(-1);
        vertexConsumer.vertex(matrix4f, x1, y1, 0.0F).texture(u2, v1).color(-1);
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
            target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"
        )
    )
    private static void melancholic_hunger$moveArmorBar(
            DrawContext drawContext, Function<Identifier, RenderLayer> renderLayers, Identifier texture, int x, int y, int width, int height, Operation<Void> original
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
        original.call(drawContext, renderLayers, texture, x, y, width, height);
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
                RenderLayer::getGuiTextured, type.getTexture(hardcore, half, blinking), x, y, 9, 9,
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
                    target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"
            )
    )
    private void melancholic_hunger$moveBubblesBar(
            DrawContext drawContext, Function<Identifier, RenderLayer> renderLayers, Identifier texture, int x, int y,
            int width, int height, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
            // move bar to the left and reverse render order from left to right
            x = drawHudContext.getMirroredX(x);
        }
        original.call(drawContext, renderLayers, texture, x, drawHudContext.getBubblesBarY(), width, height);
    }
}