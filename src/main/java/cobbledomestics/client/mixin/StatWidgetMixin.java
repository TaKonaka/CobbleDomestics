package cobbledomestics.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.cobblemon.mod.common.client.gui.summary.widgets.screens.stats.StatWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.client.gui.HumorFeatureRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Inserts the Mimos (humor) bar into Summary → Otros, directly under friendship.
 */
@Mixin(StatWidget.class)
public abstract class StatWidgetMixin {
	@Shadow
	@Final
	private Pokemon pokemon;

	@Inject(
			method = "renderWidget",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/List;addAll(ILjava/util/Collection;)Z",
					shift = At.Shift.AFTER),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void cobbledomestics$insertHumorBar(
			GuiGraphics context,
			int mouseX,
			int mouseY,
			float partialTicks,
			CallbackInfo ci,
			boolean renderOtherStats,
			boolean renderPentagonStats,
			com.mojang.blaze3d.vertex.PoseStack matrices,
			int barOffsetY,
			float barPosX,
			float drawY,
			java.util.List featuresList) {
		if (!renderOtherStats || this.pokemon == null) {
			return;
		}
		if (this.pokemon.getAspects().contains("hide-humor")) {
			return;
		}
		int insertAt = Math.min(1, featuresList.size());
		featuresList.add(insertAt, new HumorFeatureRenderer(this.pokemon));
	}
}
