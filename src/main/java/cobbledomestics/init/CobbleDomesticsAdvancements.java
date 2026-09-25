package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;

public final class CobbleDomesticsAdvancements {
	public static final ResourceLocation TIDE_POD = id("tide_pod_challenge");
	public static final ResourceLocation SMELLS_BAD = id("smells_a_bit_bad");
	public static final ResourceLocation BATH_TIME = id("bath_time");
	public static final ResourceLocation NEW_PARTNER = id("new_partner");

	private CobbleDomesticsAdvancements() {
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, path);
	}

	public static void award(ServerPlayer player, ResourceLocation advancementId) {
		AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
		if (holder == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		if (progress.isDone()) {
			return;
		}
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(holder, criterion);
		}
	}
}
