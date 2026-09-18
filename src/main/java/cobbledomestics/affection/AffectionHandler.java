package cobbledomestics.affection;

import java.util.UUID;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.animation.InteractionAnimations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class AffectionHandler {
	private static final double MAX_INTERACT_DISTANCE = 8.0;
	private static final String[] HUMOR_FAIL_KEYS = {
			"message.cobbledomestics.affection.humor.fail.0",
			"message.cobbledomestics.affection.humor.fail.1",
			"message.cobbledomestics.affection.humor.fail.2",
			"message.cobbledomestics.affection.humor.fail.3",
			"message.cobbledomestics.affection.humor.fail.4",
			"message.cobbledomestics.affection.humor.fail.5",
			"message.cobbledomestics.affection.humor.fail.6",
			"message.cobbledomestics.affection.humor.fail.7"
	};
	private static final String[] CONFIANZA_FAIL_KEYS = {
			"message.cobbledomestics.affection.confianza.fail.0",
			"message.cobbledomestics.affection.confianza.fail.1",
			"message.cobbledomestics.affection.confianza.fail.2",
			"message.cobbledomestics.affection.confianza.fail.3"
	};

	private AffectionHandler() {
	}

	/**
	 * For non-owners (wild or someone else's Pokémon), cancel Shift+use so Cobblemon
	 * does not mount/ride; the client opens the affection-only Interact Wheel instead.
	 */
	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		if (event.getHand() != InteractionHand.MAIN_HAND) {
			return;
		}
		if (!event.getEntity().isShiftKeyDown()) {
			return;
		}
		if (!(event.getTarget() instanceof PokemonEntity pokemonEntity)) {
			return;
		}
		Pokemon pokemon = pokemonEntity.getPokemon();
		if (AffectionData.isOwnedBy(pokemon, event.getEntity().getUUID())) {
			return;
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
	}

	public static void handleAction(ServerPlayer player, UUID pokemonEntityId, AffectionAction action) {
		Entity entity = player.serverLevel().getEntity(pokemonEntityId);
		if (!(entity instanceof PokemonEntity pokemonEntity)) {
			return;
		}
		if (player.distanceTo(pokemonEntity) > MAX_INTERACT_DISTANCE) {
			return;
		}
		if (pokemonEntity.isBattling()) {
			return;
		}

		Pokemon pokemon = pokemonEntity.getPokemon();
		switch (action) {
			case CARICIA -> tryCaricia(player, pokemonEntity, pokemon);
			case ABRAZO -> tryAbrazo(player, pokemonEntity, pokemon);
		}
	}

	private static void tryCaricia(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon) {
		if (!AffectionData.trySpendHumor(pokemon, AffectionData.CARICIA_HUMOR_COST)) {
			failHumor(player, pokemonEntity, pokemon);
			return;
		}
		int reward = AffectionData.rollCariciaReward(player.getRandom());
		applyReward(player, pokemonEntity, pokemon, reward);
		playSuccess(pokemonEntity);
	}

	private static void tryAbrazo(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon) {
		if (!AffectionData.canAbrazoConfianza(pokemon)) {
			failConfianza(player, pokemonEntity, pokemon);
			return;
		}
		if (!AffectionData.trySpendHumor(pokemon, AffectionData.ABRAZO_HUMOR_COST)) {
			failHumor(player, pokemonEntity, pokemon);
			return;
		}
		int reward = AffectionData.rollAbrazoReward(player.getRandom());
		applyReward(player, pokemonEntity, pokemon, reward);
		playSuccess(pokemonEntity);
	}

	private static void applyReward(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon, int reward) {
		if (AffectionData.isWild(pokemon)) {
			AffectionData.addConfianza(pokemon, reward);
			if (AffectionData.getConfianza(pokemon) >= AffectionData.MAX_CONFIANZA) {
				JoinTeamHandler.offerJoin(player, pokemonEntity);
			}
		} else if (reward > 0) {
			pokemon.incrementFriendship(reward, true);
		}
	}

	private static void failHumor(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon) {
		String key = HUMOR_FAIL_KEYS[player.getRandom().nextInt(HUMOR_FAIL_KEYS.length)];
		tell(player, Component.translatable(key, name(pokemon)));
		playFail(pokemonEntity);
	}

	private static void failConfianza(ServerPlayer player, PokemonEntity pokemonEntity, Pokemon pokemon) {
		String key = CONFIANZA_FAIL_KEYS[player.getRandom().nextInt(CONFIANZA_FAIL_KEYS.length)];
		tell(player, Component.translatable(key, name(pokemon)));
		playFail(pokemonEntity);
	}

	private static void playSuccess(PokemonEntity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.45, 0.35, 0.45, 0.02);
		}
		InteractionAnimations.playOnPokemon(entity, "cry");
	}

	private static void playFail(PokemonEntity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 8, 0.35, 0.3, 0.35, 0.02);
		}
	}

	private static void tell(Player player, Component message) {
		if (!player.level().isClientSide) {
			player.displayClientMessage(message, true);
		}
	}

	private static Component name(Pokemon pokemon) {
		return pokemon.getDisplayName(true);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (event.getServer().getTickCount() % AffectionData.HUMOR_REGEN_INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			var party = PlayerExtensionsKt.party(player);
			for (Pokemon pokemon : party) {
				if (pokemon != null) {
					AffectionData.regenHumor(pokemon);
				}
			}
		}
		for (ServerLevel level : event.getServer().getAllLevels()) {
			for (Entity entity : level.getEntities().getAll()) {
				if (entity instanceof PokemonEntity pokemonEntity) {
					Pokemon pokemon = pokemonEntity.getPokemon();
					if (AffectionData.isWild(pokemon)) {
						AffectionData.regenHumor(pokemon);
					}
				}
			}
		}
	}
}
