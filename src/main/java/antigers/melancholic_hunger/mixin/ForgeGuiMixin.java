package antigers.melancholic_hunger.mixin;

import antigers.melancholic_hunger.config.YACLConfig;
import antigers.melancholic_hunger.hud.DrawHudContext;
import antigers.melancholic_hunger.hud.ExperienceBarAnimation;
import antigers.melancholic_hunger.hud.ExperienceHudRenderer;
import antigers.melancholic_hunger.hud.RestoredHeartsDrawHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ForgeGui.class)
public class ForgeGuiMixin extends Gui {
	@Shadow(remap = false)
	public int leftHeight;

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
	private void melancholic_hunger$replaceDrawContext(GuiGraphics drawContext, float partialTick, Operation<Void> original) {
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
		var drawHudContext = new DrawHudContext(
				this.minecraft, drawContext.pose(), drawContext.bufferSource, drawRestoredHeartsHelper,
				this.minecraft.player.jumpableVehicle() != null
						? 7 : experienceBarAnimation.getCurrentPos(), hasMountHealth, mountHealthRows
		);
		original.call(drawHudContext, partialTick);
	}

	/**
	 * Calculates positions of armor and bubbles bars
	 */
	@WrapMethod(method="renderArmor", remap=false)
	private void melancholic_hunger$wrapRenderArmor(
			GuiGraphics guiGraphics, int x, int y, Operation<Void> original
	) {
		DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
		drawHudContext.prepareArmorAndBubblesBarsDrawing(y - leftHeight + 10);
		original.call(guiGraphics, x, y);
	}

	@WrapOperation(
			method = "renderArmor",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderArmor(
			GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move armor bar on the right side and down because hunger and experience bars are disabled
		DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
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
		int textureWidth = 256, textureHeight = 256;
		if (newArmorTexture != null) {
			atlasLocation = newArmorTexture;
			uOffset = 0;
			vOffset = 0;
			textureWidth = 9;
			textureHeight = 9;
		}
		guiGraphics.blit(atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
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

	@WrapOperation(
			method = "renderAir",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderAir(
			GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move air bubbles on top of health rows
		DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
		if (
				!(drawHudContext.getShouldRenderStamina() && drawHudContext.getShouldRenderStaminaInPlaceOfHunger())
						&& YACLConfig.hideHungerBar() && !drawHudContext.getHasMountHealth()
		) {
			// move bar to the left and reverse render order from left to right
			x = drawHudContext.getMirroredX(x);
		}
		y = drawHudContext.getBubblesBarY();
		original.call(guiGraphics, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
	}

	@WrapOperation(
			method = "renderFood",
			at = @At(
					value="INVOKE",
					target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
			)
	)
	private void melancholic_hunger$modifyRenderFood(
			GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, Operation<Void> original
	) {
		// Move air bubbles on top of health rows
		DrawHudContext drawHudContext = (DrawHudContext) guiGraphics;
		// Disables hunger bar rendering or moves it down if experience bar is disabled
		if (!YACLConfig.hideHungerBar()) {
			// hunger bar is drawn at the same height as health bar
			y = drawHudContext.getHealthBarY();
			original.call(guiGraphics, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
		}
		drawHudContext.renderStamina();
	}

	/**
	 * Moves mount health bar according to the exp bar animation position if there is no mount jump bar
	 */
	@WrapMethod(method="renderHealthMount", remap=false)
	private void melancholic_hunger$moveMountHealthBar(int x, int y, GuiGraphics guiGraphics, Operation<Void> original) {
		if (this.minecraft.player.jumpableVehicle() == null) {
			y -= ((ExperienceHudRenderer)this).melancholic_hunger$getExperienceBarAnimation().getCurrentPos() - 7;
		}
		original.call(x, y, guiGraphics);
	}
}
