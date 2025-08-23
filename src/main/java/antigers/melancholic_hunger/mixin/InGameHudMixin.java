package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.MelancholicHungerClient;
import antigers.melancholic_hunger.compat.RaisedCompat;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

@Mixin(Gui.class)
public abstract class InGameHudMixin implements ExperienceHudRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private void renderExperienceLevel(GuiGraphics context, DeltaTracker tickCounter) {};
    @Shadow @Nullable protected abstract Player getCameraPlayer();
    @Shadow private void renderExperienceBar(GuiGraphics context, int x) {};
    @Shadow protected abstract boolean isExperienceBarVisible();
    @Shadow protected abstract int getVehicleMaxHearts(@Nullable LivingEntity entity);
    @Shadow protected abstract int getVisibleVehicleHeartRows(int heartCount);
    @Shadow @Nullable protected abstract LivingEntity getPlayerVehicleWithHealth();
    @Shadow @Final private RandomSource random;
    @Shadow @Final private static ResourceLocation ARMOR_EMPTY_SPRITE;
    @Shadow @Final private static ResourceLocation ARMOR_HALF_SPRITE;
    @Unique private static final ResourceLocation VANILLA_ARMOR_EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_empty"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_half"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE_INVERSED = ResourceLocation.fromNamespaceAndPath(
            "melancholic_hunger", "hud/armor_half_inversed"
    );
    @Unique private final ExperienceBarAnimation melancholic_hunger$experienceBarAnimation = new ExperienceBarAnimation();

    /**
     * Changes height of the experience bar according to the animation position
     */
    @WrapOperation(
            method = "renderExperienceBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"
            )
    )
    private int melancholic_hunger$modifyExpBarY(GuiGraphics instance, Operation<Integer> original) {
        return original.call(instance) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
    }

    /**
     * Changes height of the experience level according to the animation position
     */
    @WrapOperation(
            method = "renderExperienceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"
            )
    )
    private int melancholic_hunger$modifyExpLevelY(GuiGraphics instance, Operation<Integer> original) {
        return original.call(instance) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
    }

    /**
     * Moves mount health bar according to the exp bar animation position if there is no mount jump bar
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
    private void melancholic_hunger$addExperienceBarShading(GuiGraphics context, int x, Operation<Void> original) {
        float currentOpacity = melancholic_hunger$experienceBarAnimation.getCurrentOpacity();
        context.setColor(1.0F, 1.0F, 1.0F, currentOpacity);
        original.call(context, x);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Sets opacity of the experience level according to the animation
     */
    @WrapMethod(
            method = "renderExperienceLevel"
    )
    private void melancholic_hunger$addExperienceLevelShading(
            GuiGraphics context, DeltaTracker tickCounter, Operation<Void> original
    ) {
        float currentOpacity = melancholic_hunger$experienceBarAnimation.getCurrentOpacity();
        context.setColor(1.0F, 1.0F, 1.0F, currentOpacity);
        original.call(context, tickCounter);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
        if (!this.isExperienceBarVisible()) {
            return false;
        }
        var currentScreen = this.minecraft.screen;
        return (
                (YACLConfig.showExperienceInInventory() && currentScreen instanceof InventoryScreen) ||
                (YACLConfig.showExperienceOnScreens() && (
                        currentScreen instanceof FurnaceScreen || currentScreen instanceof BlastFurnaceScreen
                        || currentScreen instanceof SmokerScreen || currentScreen instanceof EnchantmentScreen
                        || currentScreen instanceof AnvilScreen || currentScreen instanceof GrindstoneScreen
                ))
        );
    }

    public void melancholic_hunger$renderExperienceHud(GuiGraphics drawContext) {
        if (YACLConfig.renderExperienceOverBackground() && melancholic_hunger$needToRenderExperienceHudOnCurrentScreen()) {
            if (MelancholicHungerClient.raisedInstalled) {
                RaisedCompat.startHotbarTranslate(drawContext);
            }
            this.renderExperienceBar(drawContext, drawContext.guiWidth() / 2 - 91);
            this.renderExperienceLevel(drawContext, null);
            if (MelancholicHungerClient.raisedInstalled) {
                RaisedCompat.endTranslate(drawContext);
            }
        }
    }

    public void melancholic_hunger$onAddExperience() {
        if (YACLConfig.showExperienceOnGain()) {
            melancholic_hunger$experienceBarAnimation.onGainExperience();
        }
    }

    /**
     * Replaces default GuiGraphics object with the custom DrawHudContext object
     */
    @WrapMethod(method="render")
    private void melancholic_hunger$replaceDrawContext(
            GuiGraphics guiGraphics, DeltaTracker deltaTracker, Operation<Void> original
    ) {
        Player playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }
        melancholic_hunger$experienceBarAnimation.update(melancholic_hunger$needToRenderExperienceHudOnCurrentScreen());
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(playerEntity, this.random);
        int mountHealthHeartCount = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getVisibleVehicleHeartRows(mountHealthHeartCount);
        var drawHudContext = new DrawHudContext(
                this.minecraft, guiGraphics.pose(), guiGraphics.bufferSource(), drawRestoredHeartsHelper,
                this.minecraft.player.jumpableVehicle() != null
                        ? 7 : melancholic_hunger$experienceBarAnimation.getCurrentPos(), hasMountHealth, mountHealthRows
        );
        original.call(drawHudContext, deltaTracker);
    }

    /**
     * Disables hunger bar rendering or moves it down if experience bar is disabled
     */
    @WrapMethod(method = "renderFood")
    private void melancholic_hunger$disableHungerBar(
            GuiGraphics guiGraphics, Player player, int y, int x, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
        if (!YACLConfig.hideHungerBar()) {
            // hunger bar is drawn at the same height as health bar
            original.call(guiGraphics, player, drawHudContext.getHealthBarY(), x);
        }
        drawHudContext.renderStamina();
    }

    /**
     * Calculates positions of armor and bubbles bars
     */
    @WrapMethod(method="renderArmor")
    private static void melancholic_hunger$wrapRenderArmor(
            GuiGraphics drawContext, Player player, int i, int j, int k, int x, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        drawHudContext.prepareArmorAndBubblesBarsDrawing(i - (j - 1) * k);
        original.call(drawContext, player, i, j, k, x);
    }

    @Unique
    private static void melancholic_hunger$drawGuiTextureInversed(
            GuiGraphics drawContext, ResourceLocation texture, int x, int y, int width, int height
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix4f = drawContext.pose().last().pose();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float u1 = sprite.getU0(), u2 = sprite.getU1();
        float v1 = sprite.getV0(), v2 = sprite.getV1();

        builder.addVertex(matrix4f, x, y + height, 0.0F).setUv(u2, v2);
        builder.addVertex(matrix4f, x + width, y + height, 0.0F).setUv(u1, v2);
        builder.addVertex(matrix4f, x + width, y, 0.0F).setUv(u1, v1);
        builder.addVertex(matrix4f, x, y, 0.0F).setUv(u2, v1);

        BufferUploader.drawWithShader(builder.buildOrThrow());
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
            target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
        )
    )
    private static void melancholic_hunger$moveArmorBar(
            GuiGraphics drawContext, ResourceLocation texture, int x, int y, int width, int height, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        y = drawHudContext.getArmorBarY();
        if (
                YACLConfig.hideHungerBar() &&
                        !drawHudContext.getShouldRenderStaminaInPlaceOfHunger() && !drawHudContext.getHasMountHealth()
        ) {
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
        original.call(drawContext, texture, x, y, width, height);
    }

    /**
     * Moves health bar down because experience bar is disabled
     */
    @WrapMethod(method="renderHearts")
    private void melancholic_hunger$moveHealthBar(
            GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth,
            int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
        original.call(
                guiGraphics, player, x, drawHudContext.getHealthBarY(), height, offsetHeartIndex, maxHealth,
                currentHealth, displayHealth, absorptionAmount, renderHighlight
        );
    }

    @Unique
    private void melancholic_hunger$drawHeartWithColor(
            GuiGraphics context, Gui.HeartType type, int x, int y, boolean hardcore, boolean blinking,
            boolean half, int colorRed, int colorGreen, int colorBlue
    ) {
        context.setColor(colorRed / 255F, colorGreen / 255F, colorBlue / 255F, 1.0F);
        context.blitSprite(type.getSprite(hardcore, half, blinking), x, y, 9, 9);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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
        method="renderAirLevel",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
        )
    )
    private void melancholic_hunger$moveBubblesBar(
            GuiGraphics drawContext, ResourceLocation texture, int x, int y, int width, int height, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        if (
                !(drawHudContext.getShouldRenderStamina() && drawHudContext.getShouldRenderStaminaInPlaceOfHunger())
                        && YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()
        ) {
            // move bar to the left and reverse render order from left to right
            x = drawHudContext.getMirroredX(x);
        }
        original.call(drawContext, texture, x, drawHudContext.getBubblesBarY(), width, height);
    }
}