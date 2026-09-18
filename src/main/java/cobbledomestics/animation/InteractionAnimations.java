package cobbledomestics.animation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;

import net.minecraft.world.entity.Entity;

/**
 * Server-side helper that plays existing posable animations on a Pokémon via
 * {@link PlayPosableAnimationPacket}. Candidates are tried in order until the
 * client finds one that exists on the current model.
 */
public final class InteractionAnimations {
	private static final double DEFAULT_VISIBILITY_RANGE = 64.0;

	private InteractionAnimations() {
	}

	public static void playOnPokemon(PokemonEntity pokemon, String... animationNames) {
		playOnPokemon(pokemon, animationNames, Collections.emptyList(), DEFAULT_VISIBILITY_RANGE);
	}

	public static void playOnPokemon(PokemonEntity pokemon, String[] animationNames, List<String> expressions, double visibilityRange) {
		Set<String> names = new LinkedHashSet<>();
		if (animationNames != null) {
			for (String name : animationNames) {
				if (name != null && !name.isBlank()) {
					names.add(name);
				}
			}
		}
		playOnEntity(pokemon, names, expressions, visibilityRange);
	}

	public static void playOnEntity(Entity entity, Collection<String> animationNames) {
		playOnEntity(entity, animationNames, Collections.emptyList(), DEFAULT_VISIBILITY_RANGE);
	}

	public static void playOnEntity(Entity entity, Collection<String> animationNames, List<String> expressions, double visibilityRange) {
		if (entity.level().isClientSide) {
			return;
		}
		if (animationNames == null || animationNames.isEmpty()) {
			return;
		}

		Set<String> names = new LinkedHashSet<>(animationNames);
		List<String> molang = expressions == null ? Collections.emptyList() : expressions;

		PlayPosableAnimationPacket packet = new PlayPosableAnimationPacket(
				entity.getId(),
				names,
				molang
		);
		packet.sendToPlayersAround(
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				visibilityRange,
				entity.level().dimension(),
				player -> false
		);
	}
}
