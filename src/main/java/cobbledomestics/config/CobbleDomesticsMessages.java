package cobbledomestics.config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Gates gameplay {@link Player#displayClientMessage} using each client's synced preference.
 */
public final class CobbleDomesticsMessages {
	private static final Map<UUID, Boolean> SHOW_MESSAGES = new ConcurrentHashMap<>();

	private CobbleDomesticsMessages() {
	}

	public static void setShowMessages(UUID playerId, boolean show) {
		SHOW_MESSAGES.put(playerId, show);
	}

	public static void clear(UUID playerId) {
		SHOW_MESSAGES.remove(playerId);
	}

	public static boolean shouldShow(Player player) {
		return SHOW_MESSAGES.getOrDefault(player.getUUID(), true);
	}

	public static void tell(Player player, Component message) {
		if (player.level().isClientSide || !shouldShow(player)) {
			return;
		}
		player.displayClientMessage(message, false);
	}
}
