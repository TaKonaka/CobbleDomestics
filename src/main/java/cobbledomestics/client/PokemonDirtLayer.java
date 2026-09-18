package cobbledomestics.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import cobbledomestics.bath.BathData;

/**
 * Ya NO añade una capa de render con la textura "Dirt" (ese enfoque generaba z-fighting).
 * Ahora solo expone el nivel de suciedad / jabonoso "visible" (aspectos sincronizados
 * primero, NBT del Pokémon como fallback). Los mixins usan estos valores para pedir
 * texturas horneadas a DirtTextureBaker / SoapTextureBaker.
 */
public final class PokemonDirtLayer {

	private PokemonDirtLayer() {
	}

	public static int visibleSuciedad(PokemonEntity entity) {
		int fromAspects = BathData.suciedadFromAspects(entity.getEntityData().get(PokemonEntity.getASPECTS()));
		if (fromAspects >= 0) {
			return fromAspects;
		}
		fromAspects = BathData.suciedadFromAspects(entity.getPokemon().getAspects());
		if (fromAspects >= 0) {
			return fromAspects;
		}
		return BathData.getSuciedad(entity.getPokemon());
	}

	public static int visibleJabonoso(PokemonEntity entity) {
		int fromAspects = BathData.jabonosoFromAspects(entity.getEntityData().get(PokemonEntity.getASPECTS()));
		if (fromAspects >= 0) {
			return fromAspects;
		}
		fromAspects = BathData.jabonosoFromAspects(entity.getPokemon().getAspects());
		if (fromAspects >= 0) {
			return fromAspects;
		}
		return BathData.getJabonoso(entity.getPokemon());
	}
}
