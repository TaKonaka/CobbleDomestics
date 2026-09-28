package cobbledomestics.config;

import cobbledomestics.CobbleDomesticsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class CobbleDomesticsConfigEvents {
	private CobbleDomesticsConfigEvents() {
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		CobbleDomesticsMessages.clear(event.getEntity().getUUID());
	}
}
