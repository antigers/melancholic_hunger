package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.MelancholicConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import antigers.melancholic_hunger.hud.RestoredHeartsDrawHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public class ForgeGuiMixin extends Gui {
	@Shadow(remap = false)
	public int leftHeight;
	@Shadow(remap = false)
	public int rightHeight;

	@Unique private static final ResourceLocation VANILLA_ARMOR_EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			"melancholic_hunger", "textures/gui/sprites/hud/armor_empty.png"
	);
	@Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			"melancholic_hunger", "textures/gui/sprites/hud/armor_half.png"
	);
	@Unique private static final ResourceLocation VANILLA_ARMOR_HALF_TEXTURE_INVERSED = ResourceLocation.fromNamespaceAndPath(
			"melancholic_hunger", "textures/gui/sprites/hud/armor_half_inversed.png"
	);

	public ForgeGuiMixin(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
		super(pMinecraft, pItemRenderer);
	}

	/**
	 * Replaces default GuiGraphics object with the custom DrawHudContext object
	 */
	@WrapMethod(method="render")
	private void melancholic_hunger$replaceDrawContext(PoseStack poseStack, float partialTick, Operation<Void> original) {
		Player playerEntity = this.getCameraPlayer();
		if (playerEntity == null) {
			return;
		}
		ExperienceHudRenderer experienceHudRenderer = ((ExperienceHudRenderer)this);
		ExperienceBarAnimation experienceBarAnimation = experienceHudRenderer.melancholic_hunger$getExperienceBarAnimation();
		experienceBarAnimation.update(experienceHudRenderer.melancholic_hunger$needToRenderExperienceHudOnCurrentScreen());
		var drawRestoredHeartsHelper = new RestoredHeartsDrawHelper(playerEntity, this.random);
		int mountHealthHeartCount = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
		boolean hasMountHealth = mountHealthHeartCount > 0;
		int mountHealthRows = this.getVisibleVehicleHeartRows(mountHealthHeartCount);
		((ExperienceHudRenderer) this).melancholic_hunger$setDrawHudContext(
				new DrawHudContext(
						this.minecraft, drawRestoredHeartsHelper,
						this.minecraft.player.isRidingJumpable() ? 7 : experienceBarAnimation.getCurrentPos(),
						hasMountHealth, mountHealthRows, screenWidth, screenHeight
				)
		);
		original.call(poseStack, partialTick);
	}

	@Inject(
			method="render",
			at=@At(
					value="INVOKE",
					target="Lnet/minecraftforge/client/event/RenderGuiEvent$Pre;<init>(Lcom/mojang/blaze3d/platform/Window;Lcom/mojang/blaze3d/vertex/PoseStack;F)V"
			)
	)
	private void melancholic_hunger$setupForgeGuiHeights(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) this).melancholic_hunger$getDrawHudContext();
		// setting up forge gui heights in accordance with experience bar offset
		int experienceOffset = drawHudContext.getHudExperienceOffset() - 7;
		leftHeight += experienceOffset;
		rightHeight += experienceOffset;
	}

	/**
	 * Calculates positions of armor and bubbles bars
	 */
	@WrapMethod(method="renderArmor", remap=false)
	private void melancholic_hunger$wrapRenderArmor(
			PoseStack poseStack, int x, int y, Operation<Void> original
	) {
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) this).melancholic_hunger$getDrawHudContext();
		drawHudContext.prepareArmorAndBubblesBarsDrawing(y - leftHeight + 10);
		original.call(poseStack, x, y);
	}

	@WrapOperation(
			method = "renderArmor",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraftforge/client/gui/overlay/ForgeGui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderArmor(
			ForgeGui instance, PoseStack poseStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move armor bar on the right side and down because hunger and experience bars are disabled
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) this).melancholic_hunger$getDrawHudContext();
		ResourceLocation newArmorTexture = null;
		y = drawHudContext.getArmorBarY();
		if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
			// move bar to the right and reverse render order from right to left
			x = drawHudContext.getMirroredX(x);
			if (!DrawHudContext.isDefaultArmorHudTexture) {
				melancholic_hunger$drawGuiTextureInversed(poseStack, x, y, uOffset, vOffset);
				return;
			}
			newArmorTexture = melancholic_hunger$fixVanillaArmorTexture(uOffset, true);
		}
		else if (DrawHudContext.isDefaultArmorHudTexture) {
			newArmorTexture = melancholic_hunger$fixVanillaArmorTexture(uOffset, false);
		}
		int textureWidth = 256, textureHeight = 256;
		if (newArmorTexture != null) {
			RenderSystem.setShaderTexture(0, newArmorTexture);
			uOffset = 0;
			vOffset = 0;
			textureWidth = 9;
			textureHeight = 9;
		}
		blit(poseStack, x, y, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
		if (newArmorTexture != null) {
			RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
		}
	}

	@Unique
	private static void melancholic_hunger$drawGuiTextureInversed(
			PoseStack poseStack, int x1, int y1, int uOffset, int vOffset
	) {
		float u1 = uOffset / 256F, u2 = (uOffset + 9F) / 256F;
		float v1 = vOffset / 256F, v2 = (vOffset + 9F) / 256F;
		int x2 = x1 + 9;
		int y2 = y1 + 9;
		// swapping u1 and u2 to rotate the texture along the vertical axis
		innerBlit(poseStack.last().pose(), x1, x2, y1, y2, 0, u2, u1, v1, v2);
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

	@WrapOperation(
			method = "renderAir",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraftforge/client/gui/overlay/ForgeGui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderAir(
			ForgeGui instance, PoseStack poseStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move air bubbles on top of health rows
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) this).melancholic_hunger$getDrawHudContext();
		if (MelancholicConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()) {
			// move bar to the left and reverse render order from left to right
			x = drawHudContext.getMirroredX(x);
		}
		y = drawHudContext.getBubblesBarY();
		original.call(instance, poseStack, x, y, uOffset, vOffset, uWidth, vHeight);
	}

	@WrapOperation(
			method = "renderFood",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraftforge/client/gui/overlay/ForgeGui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderFood(
			ForgeGui forgeGui, PoseStack poseStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move air bubbles on top of health rows
		DrawHudContext drawHudContext = ((ExperienceHudRenderer) this).melancholic_hunger$getDrawHudContext();
		// Disables hunger bar rendering or moves it down if experience bar is disabled
		if (!MelancholicConfig.hideHungerBar()) {
			// hunger bar is drawn at the same height as health bar
			y = drawHudContext.getHealthBarY();
			original.call(forgeGui, poseStack, x, y, uOffset, vOffset, uWidth, vHeight);
		}
	}
}
