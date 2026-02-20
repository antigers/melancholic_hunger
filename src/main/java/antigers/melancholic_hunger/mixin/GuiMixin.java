package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.InstalledMods;
import antigers.melancholic_hunger.compat.RaisedCompat;
import antigers.melancholic_hunger.compat.farmers_delight.NourishmentEffectHandler;
import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
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
public abstract class GuiMixin implements ExperienceHudRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable protected abstract Player getCameraPlayer();
    @Shadow private void renderExperienceBar(GuiGraphics context, int x) {};
    @Shadow protected abstract int getVehicleMaxHearts(@Nullable LivingEntity entity);
    @Shadow protected abstract int getVisibleVehicleHeartRows(int heartCount);
    @Shadow @Nullable protected abstract LivingEntity getPlayerVehicleWithHealth();
    @Shadow @Final private RandomSource random;
    @Shadow @Final private static ResourceLocation GUI_ICONS_LOCATION;
    @Unique private static final ResourceLocation VANILLA_ARMOR_EMPTY_TEXTURE = new ResourceLocation(
            "melancholic_hunger", "textures/gui/sprites/hud/armor_empty.png"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE = new ResourceLocation(
            "melancholic_hunger", "textures/gui/sprites/hud/armor_half.png"
    );
    @Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE_INVERSED = new ResourceLocation(
            "melancholic_hunger", "textures/gui/sprites/hud/armor_half_inversed.png"
    );
    @Unique private final ExperienceBarAnimation melancholic_hunger$experienceBarAnimation = new ExperienceBarAnimation();

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

    /**
     * Moves mount health bar according to the exp bar animation position if there is no mount jump bar
     */
    @WrapOperation(
            method="renderVehicleHealth",
            at=@At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/Gui;screenHeight:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int melancholic_hunger$moveMountHealthBar(Gui instance, Operation<Integer> original) {
        if (this.minecraft.player.jumpableVehicle() == null) {
            return original.call(instance) - melancholic_hunger$experienceBarAnimation.getCurrentPos() + 7;
        }
        return original.call(instance);
    }

    /**
     * Sets opacity of the experience bar according to the animation
     */
    @WrapOperation(
            method = "renderExperienceBar",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            )
    )
    private void melancholic_hunger$addExperienceBarShading(
            GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x1, int y1, int uOffset, int vOffset, int uWidth, int vHeight,
            Operation<Void> original
    ) {
        float u1 = uOffset / 256F, u2 = (uOffset + uWidth) / 256F;
        float v1 = vOffset / 256F, v2 = (vOffset + vHeight) / 256F;
        int x2 = x1 + uWidth;
        int y2 = y1 + vHeight;
        float currentOpacity = melancholic_hunger$experienceBarAnimation.getCurrentOpacity();
        guiGraphics.innerBlit(atlasLocation, x1, x2, y1, y2, 0, u1, u2, v1, v2, 1.0F, 1.0F, 1.0F, currentOpacity);
    }

    /**
     * Sets opacity of the experience level according to the animation
     */
    @WrapMethod(
            method = "renderExperienceBar"
    )
    private void melancholic_hunger$addExperienceLevelShading(GuiGraphics context, int x, Operation<Void> original) {
        float currentOpacity = melancholic_hunger$experienceBarAnimation.getCurrentOpacity();
        context.setColor(1.0F, 1.0F, 1.0F, currentOpacity);
        original.call(context, x);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Unique
    public boolean melancholic_hunger$needToRenderExperienceHudOnCurrentScreen() {
        // rendering only if some certain interface screen is open (inventory, enchantment table, etc.)
        if (this.minecraft.gameMode == null || !this.minecraft.gameMode.hasExperience()) {
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
            if (InstalledMods.RAISED) {
                RaisedCompat.startHotbarTranslate(drawContext);
            }
            this.renderExperienceBar(drawContext, drawContext.guiWidth() / 2 - 91);
            if (InstalledMods.RAISED) {
                RaisedCompat.endTranslate(drawContext);
            }
        }
    }

    public void melancholic_hunger$onAddExperience() {
        if (YACLConfig.showExperienceOnGain()) {
            melancholic_hunger$experienceBarAnimation.onGainExperience();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void melancholic_hunger$calculateHudOffset(
            GuiGraphics guiGraphics, float partialTick, CallbackInfo ci
    ) {
        melancholic_hunger$experienceBarAnimation.update(melancholic_hunger$needToRenderExperienceHudOnCurrentScreen());
    }

    /**
     * Replaces default GuiGraphics object with the custom DrawHudContext object
     */
    @WrapOperation(
            method="render",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V"
            )
    )
    private void melancholic_hunger$replaceDrawContext(Gui inGameHud, GuiGraphics drawContext, Operation<Void> original) {
        Player playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }
        var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(playerEntity, this.random);
        int mountHealthHeartCount = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
        boolean hasMountHealth = mountHealthHeartCount > 0;
        int mountHealthRows = this.getVisibleVehicleHeartRows(mountHealthHeartCount);
        var drawHudContext = new DrawHudContext(
                this.minecraft, drawContext.pose(), drawContext.bufferSource, drawRestoredHeartsHelper,
                this.minecraft.player.jumpableVehicle() != null
                        ? 7 : melancholic_hunger$experienceBarAnimation.getCurrentPos(), hasMountHealth, mountHealthRows
        );
        original.call(inGameHud, drawHudContext);
    }

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            )
    )
    private void melancholic_hunger$modifyDrawnTexture(
            GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
        int textureWidth = 256, textureHeight = 256;
        // armor bar
        if (vOffset == 9) {
            // Move armor bar on the right side and down because hunger and experience bars are disabled
            ResourceLocation newArmorTexture = null;
            y = drawHudContext.getArmorBarY();
            if (
                    YACLConfig.hideHungerBar() &&
                            !drawHudContext.getShouldRenderStaminaInPlaceOfHunger() && !drawHudContext.getHasMountHealth()
            ) {
                // move bar to the right and reverse render order from right to left
                x = drawHudContext.getMirroredX(x);
                if (!DrawHudContext.isDefaultArmorHudTexture) {
                    melancholic_hunger$drawGuiTextureInversed(guiGraphics, atlasLocation, x, y, uOffset, vOffset);
                    return;
                }
                newArmorTexture = melancholic_hunger$fixVanillaArmorTexture(uOffset, true);
            }
            else if (DrawHudContext.isDefaultArmorHudTexture) {
                newArmorTexture = melancholic_hunger$fixVanillaArmorTexture(uOffset, false);
            }
            if (newArmorTexture != null) {
                atlasLocation = newArmorTexture;
                uOffset = 0;
                vOffset = 0;
                textureWidth = 9;
                textureHeight = 9;
            }
        }
        // bubbles bar
        else if (vOffset == 18) {
            // Move air bubbles on top of health rows
            if (
                    !(drawHudContext.getShouldRenderStamina() && drawHudContext.getShouldRenderStaminaInPlaceOfHunger())
                            && YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()
            ) {
                // move bar to the left and reverse render order from left to right
                x = drawHudContext.getMirroredX(x);
            }
            y = drawHudContext.getBubblesBarY();
        }
        // hunger bar
        else if (vOffset == 27) {
            // Disables hunger bar rendering or moves it down if experience bar is disabled
            if (!YACLConfig.hideHungerBar()) {
                // hunger bar is drawn at the same height as health bar
                y = drawHudContext.getHealthBarY();
            } else {
                return;
            }
        }
        guiGraphics.blit(atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    /**
     * Calculates positions of armor and bubbles bars
     */
    @Inject(
            method="renderPlayerHealth",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"
            )
    )
    private void melancholic_hunger$wrapRenderArmor(GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal=9) int s) {
        DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
        drawHudContext.prepareArmorAndBubblesBarsDrawing(s + 10);
    }

    @Unique
    private static void melancholic_hunger$drawGuiTextureInversed(
            GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x1, int y1, int uOffset, int vOffset
    ) {
        float u1 = uOffset / 256F, u2 = (uOffset + 9F) / 256F;
        float v1 = vOffset / 256F, v2 = (vOffset + 9F) / 256F;
        int x2 = x1 + 9;
        int y2 = y1 + 9;
        // swapping u1 and u2 to rotate the texture along the vertical axis
        guiGraphics.innerBlit(atlasLocation, x1, x2, y1, y2, 0, u2, u1, v1, v2);
    }

    @Unique
    private static ResourceLocation melancholic_hunger$fixVanillaArmorTexture(int uOffset, boolean inversed) {
        if (uOffset == 25) {
            return inversed ? VANILLA_ARMOR_HALF_TEXTURE_INVERSED : VANILLA_ARMOR_HALF_TEXTURE;
        }
        if (uOffset == 16) {
            return VANILLA_ARMOR_EMPTY_TEXTURE;
        }
        return null;
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
        drawHudContext.renderStamina();
    }

    @Unique
    private void melancholic_hunger$drawHeartWithColor(
            GuiGraphics context, Gui.HeartType type, int x, int y, int yOffset, boolean renderHighlight, boolean halfHeart,
            int colorRed, int colorGreen, int colorBlue
    ) {
        context.setColor(colorRed / 255F, colorGreen / 255F, colorBlue / 255F, 1.0F);
        context.blit(GUI_ICONS_LOCATION, x, y, type.getX(halfHeart, renderHighlight), yOffset, 9, 9);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws amount of hearts that can be restored by eating currently held food item
     */
    @WrapOperation(
            method="renderHearts",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIIZZ)V"
            )
    )
    private void melancholic_hunger$drawRestoredHearts(
            Gui inGameHud, GuiGraphics drawContext, Gui.HeartType type, int x, int y, int yOffset, boolean renderHighlight,
            boolean halfHeart, Operation<Void> original
    ) {
        DrawHudContext drawHudContext = (DrawHudContext) drawContext;
        RestoredHeartsDrawHelper restoredHeartsDrawHelper = drawHudContext.getHelper();
        if (type != Gui.HeartType.CONTAINER) {
            original.call(
                    inGameHud, drawContext, type, x, restoredHeartsDrawHelper.getCurrentY(), yOffset, renderHighlight, halfHeart
            );
            return;
        }
        y = restoredHeartsDrawHelper.updateCurrentY(y);
        var res = restoredHeartsDrawHelper.heartsToDraw();
        RestoredHeartsDrawHelper.RestoredHeart firstHeart = res.getFirst();
        if (firstHeart != null) {
            // drawing container for correct background
            original.call(inGameHud, drawContext, Gui.HeartType.CONTAINER, x, y, yOffset, renderHighlight, halfHeart);
            melancholic_hunger$drawHeartWithColor(
                    drawContext, firstHeart.heartType(), x, y, yOffset, renderHighlight, firstHeart.isHalf(),
                    firstHeart.colorRed(), firstHeart.colorGreen(), firstHeart.colorBlue()
            );
            RestoredHeartsDrawHelper.RestoredHeart secondHeart = res.getSecond();
            if (secondHeart != null) {
                // drawing second heart on top of the first
                melancholic_hunger$drawHeartWithColor(
                        drawContext, secondHeart.heartType(), x, y, yOffset, renderHighlight, secondHeart.isHalf(),
                        secondHeart.colorRed(), secondHeart.colorGreen(), secondHeart.colorBlue()
                );
            }
        }
        else {
            original.call(inGameHud, drawContext, type, x, y, yOffset, renderHighlight, halfHeart);
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