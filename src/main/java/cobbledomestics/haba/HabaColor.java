package cobbledomestics.haba;

import java.util.Set;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;

public enum HabaColor {
	RED(ElementalTypes.FIRE, ElementalTypes.PSYCHIC),
	PURPLE(ElementalTypes.FAIRY, ElementalTypes.GHOST, ElementalTypes.DARK),
	ORANGE(ElementalTypes.FIGHTING, ElementalTypes.ROCK),
	GREEN(ElementalTypes.NORMAL, ElementalTypes.GRASS, ElementalTypes.BUG),
	CIAN(ElementalTypes.DRAGON, ElementalTypes.ICE, ElementalTypes.FLYING),
	BLUE(ElementalTypes.WATER, ElementalTypes.POISON),
	YELLOW(ElementalTypes.ELECTRIC, ElementalTypes.GROUND);

	private final Set<ElementalType> matchingTypes;

	HabaColor(ElementalType... types) {
		this.matchingTypes = Set.of(types);
	}

	public boolean matches(Pokemon pokemon) {
		for (ElementalType type : pokemon.getTypes()) {
			if (matchingTypes.contains(type)) {
				return true;
			}
		}
		return false;
	}

	public String idSuffix() {
		return name().toLowerCase();
	}
}
