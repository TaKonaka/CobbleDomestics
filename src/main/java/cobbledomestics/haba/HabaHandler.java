package cobbledomestics.haba;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionData;
import cobbledomestics.affection.JoinTeamHandler;
import cobbledomestics.animation.InteractionAnimations;
import cobbledomestics.item.HabaItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class HabaHandler {
	private HabaHandler() {
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof PokemonEntity pokemonEntity)) {
			return;
		}
		Player player = event.getEntity();
		if (player.isShiftKeyDown()) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof HabaItem habaItem)) {
			return;
		}
		if (event.getLevel().isClientSide()) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		InteractionResult result = tryFeed(serverPlayer, pokemonEntity, stack, habaItem);
		event.setCanceled(true);
		event.setCancellationResult(result);
	}

	private static InteractionResult tryFeed(ServerPlayer player, PokemonEntity pokemonEntity, ItemStack stack, HabaItem habaItem) {
		Pokemon pokemon = pokemonEntity.getPokemon();
		HabaTier tier = habaItem.getTier();
		int cost = tier.satietyCost();
		int current = pokemon.getCurrentFullness();
		int max = pokemon.getMaxFullness();

		if (current + cost > max) {
			player.displayClientMessage(Component.translatable("message.cobbledomestics.haba.full", pokemon.getDisplayName(true)), true);
			playFail(pokemonEntity);
			return InteractionResult.FAIL;
		}

		boolean typeMatch = tier.hasTypeBonus() && habaItem.getColor() != null && habaItem.getColor().matches(pokemon);
		int amistad = tier.baseAmistad() + (typeMatch ? tier.extraAmistad() : 0);
		int confianza = tier.baseConfianza() + (typeMatch ? tier.extraConfianza() : 0);
		boolean wild = AffectionData.isWild(pokemon);

		if (!wild && amistad > 0) {
			pokemon.incrementFriendship(amistad, true);
		}

		if (wild && confianza > 0) {
			AffectionData.addConfianza(pokemon, confianza);
			if (AffectionData.getConfianza(pokemon) >= AffectionData.getLvCaptura(pokemon)) {
				JoinTeamHandler.offerJoin(player, pokemonEntity);
			}
		}

		pokemon.feedPokemon(cost, true);
		stack.consume(1, player);

		if (wild) {
			player.displayClientMessage(Component.translatable(
					"message.cobbledomestics.haba.fed_wild",
					pokemon.getDisplayName(true),
					confianza), true);
		} else {
			player.displayClientMessage(Component.translatable(
					"message.cobbledomestics.haba.fed_owned",
					pokemon.getDisplayName(true),
					amistad), true);
		}

		playSuccess(pokemonEntity);
		return InteractionResult.SUCCESS;
	}

	private static void playSuccess(PokemonEntity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.45, 0.35, 0.45, 0.02);
			serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8F, 1.1F);
		}
		InteractionAnimations.playOnPokemon(entity, "cry");
	}

	private static void playFail(PokemonEntity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 6, 0.25, 0.2, 0.25, 0.01);
		}
		InteractionAnimations.playOnPokemon(entity, "cry");
	}
}
