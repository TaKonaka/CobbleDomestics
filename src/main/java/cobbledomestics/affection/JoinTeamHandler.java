package cobbledomestics.affection;

import java.util.UUID;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import cobbledomestics.affection.network.JoinOfferPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class JoinTeamHandler {
	private static final double MAX_INTERACT_DISTANCE = 8.0;

	private JoinTeamHandler() {
	}

	public static void offerJoin(ServerPlayer player, PokemonEntity pokemonEntity) {
		Pokemon pokemon = pokemonEntity.getPokemon();
		if (!AffectionData.isWild(pokemon)) {
			return;
		}
		if (AffectionData.getConfianza(pokemon) < AffectionData.getLvCaptura(pokemon)) {
			return;
		}
		PacketDistributor.sendToPlayer(player, new JoinOfferPacket(pokemonEntity.getUUID(), pokemon.getDisplayName(true).getString()));
	}

	public static void accept(ServerPlayer player, UUID pokemonEntityId) {
		PokemonEntity pokemonEntity = resolveEligible(player, pokemonEntityId);
		if (pokemonEntity == null) {
			return;
		}

		ItemStack ballStack = findPokeBallStack(player);
		if (ballStack == null || !(ballStack.getItem() instanceof PokeBallItem pokeBallItem)) {
			player.displayClientMessage(Component.translatable("message.cobbledomestics.join.no_ball"), true);
			offerJoin(player, pokemonEntity);
			return;
		}

		PokeBall pokeBall = pokeBallItem.getPokeBall();
		Pokemon pokemon = pokemonEntity.getPokemon();
		pokemon.setCaughtBall(pokeBall);

		boolean added = PlayerExtensionsKt.party(player).add(pokemon);
		if (!added) {
			player.displayClientMessage(Component.translatable("message.cobbledomestics.join.failed", pokemon.getDisplayName(true)), true);
			offerJoin(player, pokemonEntity);
			return;
		}

		ballStack.consume(1, player);
		pokemonEntity.discard();
	}

	public static void reject(ServerPlayer player, UUID pokemonEntityId) {
		// Confianza stays at max; next successful pet/hug re-offers.
	}

	private static PokemonEntity resolveEligible(ServerPlayer player, UUID pokemonEntityId) {
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
		Pokemon pokemon = pokemonEntity.getPokemon();
		if (!AffectionData.isWild(pokemon)) {
			return null;
		}
		if (AffectionData.getConfianza(pokemon) < AffectionData.getLvCaptura(pokemon)) {
			return null;
		}
		return pokemonEntity;
	}

	/**
	 * Prefer main hand, then offhand, then first Poké Ball in the inventory.
	 */
	private static ItemStack findPokeBallStack(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof PokeBallItem) {
			return main;
		}
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof PokeBallItem) {
			return off;
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof PokeBallItem) {
				return stack;
			}
		}
		return null;
	}
}
