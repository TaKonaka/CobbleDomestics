package cobbledomestics.client.gui;

import com.cobblemon.mod.common.client.gui.summary.featurerenderers.BarSummarySpeciesFeatureRenderer;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.affection.AffectionData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Summary → Otros bar for humor ("Mimos"), shown under friendship.
 */
public class HumorFeatureRenderer extends BarSummarySpeciesFeatureRenderer {
	private static final ResourceLocation UNDERLAY = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/summary/summary_stats_other_bar.png");
	private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("cobbledomestics", "textures/gui/summary/summary_stats_mimos_overlay.png");
	/** Pastel yellow fill. */
	private static final Vec3 BAR_COLOUR = new Vec3(255.0, 232.0, 150.0);

	public HumorFeatureRenderer(Pokemon pokemon) {
		super(
				"humor",
				Component.translatable("cobbledomestics.ui.stats.mimos"),
				UNDERLAY,
				OVERLAY,
				pokemon,
				0,
				AffectionData.MAX_HUMOR,
				AffectionData.getHumor(pokemon),
				BAR_COLOUR);
	}

	@Override
	public boolean render(GuiGraphics guiGraphics, float x, float y, Pokemon pokemon) {
		renderElement(guiGraphics, x, y, pokemon, AffectionData.getHumor(pokemon));
		return true;
	}
}
