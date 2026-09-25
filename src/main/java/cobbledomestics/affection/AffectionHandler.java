package cobbledomestics.affection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.network.RubAttackPacket;
import cobbledomestics.affection.network.RubHintPacket;
import cobbledomestics.animation.InteractionAnimations;
import cobbledomestics.init.CobbleDomesticsModSounds;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class AffectionHandler {
	private static final double MAX_INTERACT_DISTANCE = 2.0;
	private static final int NOTE_THROTTLE_TICKS = 20;
	private static final int HINT_THROTTLE_TICKS = 2;

	private static final Map<UUID, RubSession> SESSIONS = new ConcurrentHashMap<>();

	private record RubSession(UUID targetEntityId, int progress, int idealTicks, int noteCooldown, int hintCooldown) {
	}

	private AffectionHandler() {
	}

	public static void handleRubTick(ServerPlayer player, UUID pokemonEntityId, int rubSpeed) {
		PokemonEntity pokemonEntity = resolvePokemon(player, pokemonEntityId);
		if (pokemonEntity == null) {
			clearSession(player);
			return;
		}

		int speed = Math.max(AffectionData.RUB_SPEED_MIN, Math.min(AffectionData.RUB_SPEED_MAX, rubSpeed));
		Pokemon pokemon = pokemonEntity.getPokemon();
		AffectionData.ensureGustos(pokemon, player.getRandom());

		RubSession session = SESSIONS.get(player.getUUID());
		if (session == null || !pokemonEntityId.equals(session.targetEntityId())) {
			session = new RubSession(pokemonEntityId, 0, 0, 0, 0);
		}

		int noteCooldown = Math.max(0, session.noteCooldown() - 1);
		int hintCooldown = Math.max(0, session.hintCooldown() - 1);
		int required = AffectionData.getActiveGustoSpeed(pokemon);
		RubHint hint = AffectionData.getRubHint(speed, required);

		if (hintCooldown <= 0) {
			PacketDistributor.sendToPlayer(player, new RubHintPacket(hint));
			hintCooldown = HINT_THROTTLE_TICKS;
		}

		boolean matching = AffectionData.matchesGustoSpeed(speed, required);
		int idealTicks = session.idealTicks() + 1;
		int progress = session.progress();
		if (idealTicks >= AffectionData.RUB_PROGRESS_INTERVAL_TICKS) {
			idealTicks = 0;
			// Idle cursor (speed 0) must not advance caricia progress.
			if (speed > 0) {
				int gain = matching ? AffectionData.RUB_PROGRESS_MATCH : AffectionData.RUB_PROGRESS_MISMATCH;
				progress = Math.min(AffectionData.RUB_PROGRESS_MAX, progress + gain);
			}
		}

		if (progress < AffectionData.RUB_PROGRESS_MAX) {
			SESSIONS.put(player.getUUID(), new RubSession(pokemonEntityId, progress, idealTicks, noteCooldown, hintCooldown));
			return;
		}

		if (AffectionData.getHumor(pokemon) >= AffectionData.MAX_HUMOR) {
			if (noteCooldown <= 0) {
				playFullHumor(pokemonEntity, pokemon);
				noteCooldown = NOTE_THROTTLE_TICKS;
			}
			SESSIONS.put(player.getUUID(), new RubSession(pokemonEntityId, 0, 0, noteCooldown, hintCooldown));
			return;
		}

		if (!AffectionData.tryAddHumor(pokemon, AffectionData.RUB_HUMOR_GAIN)) {
			SESSIONS.put(player.getUUID(), new RubSession(pokemonEntityId, 0, 0, noteCooldown, hintCooldown));
			return;
		}

		applyReward(player, pokemonEntity, pokemon);
		playSuccess(pokemonEntity);
		AffectionData.advanceGustoAfterSuccess(pokemon, player.getRandom());
		// Reset progress counter after a successful pet so the next caricia starts from 0.
		clearSession(player);
	}

	public static void handleRubEnd(ServerPlayer player) {
		clearSession(player);
	}

	public static void handleRubPoke(ServerPlayer player, UUID pokemonEntityId) {
		PokemonEntity pokemonEntity = resolvePokemon(player, pokemonEntityId);
		if (pokemonEntity == null) {
			return;
		}
		clearSession(player);
		Pokemon pokeMon = pokemonEntity.getPokemon();
		var st = pokeMon.getStatus();
		if (st != null && st.getStatus() == com.cobblemon.mod.common.api.pokemon.status.Statuses.SLEEP) {
			pokeMon.setStatus(null);
		}
		InteractionAnimations.playOnPokemon(pokemonEntity, "cry");
		if (pokemonEntity.level() instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
					CobbleDomesticsModSounds.GOLPE.get(), SoundSource.NEUTRAL, 0.9F, 1.0F);
		}
		PacketDistributor.sendToPlayer(player, new RubAttackPacket());
	}

	private static PokemonEntity resolvePokemon(ServerPlayer player, UUID pokemonEntityId) {
		Entity entity = player.serverLevel().getEntity(pokemonEntityId);
		if (!(entity instanceof PokemonEntity pokemonEntity)) {
			return null;
		}
		if (player.distanceTo(pokemonEntity) > MAX_INTERACT_DISTANCE) {
			return null;
		}
		if (pokemonEntity.isBattling()) {
			return null;
		}
		return pokemonEntity;
	}

	private static void clearSession(ServerPlayer player) {
		SESSIONS.remove(player.getUUID());
	}

	private static void applyReward(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon) {
		if (AffectionData.isWild(pokemon)) {
			AffectionData.addConfianza(pokemon, AffectionData.RUB_CONFIANZA_REWARD);
			if (AffectionData.getConfianza(pokemon) >= AffectionData.getLvCaptura(pokemon)) {
				JoinTeamHandler.offerJoin(player, pokemonEntity);
			}
		} else {
			pokemon.incrementFriendship(AffectionData.RUB_FRIENDSHIP_REWARD, true);
		}
	}

	private static void playSuccess(PokemonEntity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.45, 0.35, 0.45, 0.02);
			serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), CobbleDomesticsModSounds.MASSAGE.get(), SoundSource.NEUTRAL, 0.7F, 1.0F);
		}
		InteractionAnimations.playOnPokemon(entity, "cry");
	}

	private static void playFullHumor(PokemonEntity entity, Pokemon pokemon) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
					CobbleDomesticsModParticleTypes.forPokemon(pokemon),
					entity.getX(),
					entity.getY() + entity.getBbHeight() * 0.5,
					entity.getZ(),
					6,
					0.35,
					0.25,
					0.35,
					0.0);
			serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), CobbleDomesticsModSounds.MASSAGE.get(), SoundSource.NEUTRAL, 0.7F, 1.0F);
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (event.getServer().getTickCount() % AffectionData.HUMOR_DECAY_INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			var party = PlayerExtensionsKt.party(player);
			for (Pokemon pokemon : party) {
				if (pokemon != null) {
					AffectionData.decayHumor(pokemon);
				}
			}
		}
		for (ServerLevel level : event.getServer().getAllLevels()) {
			for (Entity entity : level.getEntities().getAll()) {
				if (entity instanceof PokemonEntity pokemonEntity) {
					Pokemon pokemon = pokemonEntity.getPokemon();
					if (AffectionData.isWild(pokemon)) {
						AffectionData.decayHumor(pokemon);
					}
				}
			}
		}
	}
}
