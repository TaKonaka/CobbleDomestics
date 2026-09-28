package cobbledomestics.affection;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * While immersive care is open, keep the target Pokémon still and looking at the player.
 */
@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class ImmersiveFocusHandler {
	private static final double MAX_FOCUS_DISTANCE = 2.0;
	private static final float LOOK_YAW_SPEED = 30.0F;
	private static final float LOOK_PITCH_SPEED = 30.0F;

	private static final Map<UUID, UUID> FOCUS_SESSIONS = new ConcurrentHashMap<>();

	private ImmersiveFocusHandler() {
	}

	public static void start(ServerPlayer player, UUID pokemonEntityId) {
		PokemonEntity pokemon = resolvePokemon(player, pokemonEntityId);
		if (pokemon == null) {
			FOCUS_SESSIONS.remove(player.getUUID());
			return;
		}
		FOCUS_SESSIONS.put(player.getUUID(), pokemonEntityId);
		holdAttention(player, pokemon);
	}

	public static void end(ServerPlayer player) {
		FOCUS_SESSIONS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (FOCUS_SESSIONS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, UUID>> it = FOCUS_SESSIONS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, UUID> entry = it.next();
			ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			PokemonEntity pokemon = resolvePokemon(player, entry.getValue());
			if (pokemon == null) {
				it.remove();
				continue;
			}
			holdAttention(player, pokemon);
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		FOCUS_SESSIONS.remove(event.getEntity().getUUID());
	}

	private static void holdAttention(ServerPlayer player, PokemonEntity pokemon) {
		pokemon.getNavigation().stop();
		Vec3 motion = pokemon.getDeltaMovement();
		pokemon.setDeltaMovement(0.0, motion.y, 0.0);
		pokemon.getLookControl().setLookAt(player, LOOK_YAW_SPEED, LOOK_PITCH_SPEED);
	}

	private static PokemonEntity resolvePokemon(ServerPlayer player, UUID pokemonEntityId) {
		Entity entity = player.serverLevel().getEntity(pokemonEntityId);
		if (!(entity instanceof PokemonEntity pokemonEntity)) {
			return null;
		}
		if (player.distanceTo(pokemonEntity) > MAX_FOCUS_DISTANCE) {
			return null;
		}
		if (pokemonEntity.isBattling()) {
			return null;
		}
		return pokemonEntity;
	}
}
