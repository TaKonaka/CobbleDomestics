package cobbledomestics.bath;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public final class BathData {
	public static final int MAX_SUCIEDAD = 5;
	public static final int MAX_JABONOSO = 5;
	public static final int SOAP_COST = 1;
	public static final int FRIENDSHIP_ROLL_MIN = 1;
	public static final int FRIENDSHIP_ROLL_MAX = 3;
	public static final String DIRT_ASPECT_PREFIX = "cobbledomestics-dirt-";
	public static final String JABONOSO_ASPECT_PREFIX = "cobbledomestics-jabonoso-";

	private static final String ROOT = "cobbledomestics";
	private static final String SUCIEDAD = "suciedad";
	private static final String SUCIEDAD_BANO = "suciedad_bano";
	private static final String JABONOSO = "jabonoso";
	private static final String MOJADO = "mojado";
	private static final String STATE = "bath_state";
	private static final String LAST_DIRT_BATTLE = "last_dirt_battle";

	private BathData() {
	}

	public static CompoundTag tag(Pokemon pokemon) {
		CompoundTag persistent = pokemon.getPersistentData();
		CompoundTag data = persistent.getCompound(ROOT);
		if (!persistent.contains(ROOT)) {
			persistent.put(ROOT, data);
		}
		return data;
	}

	public static int getSuciedad(Pokemon pokemon) {
		return clamp(tag(pokemon).getInt(SUCIEDAD), 0, MAX_SUCIEDAD);
	}

	public static int getJabonoso(Pokemon pokemon) {
		return clamp(tag(pokemon).getInt(JABONOSO), 0, MAX_JABONOSO);
	}

	public static int getMojado(Pokemon pokemon) {
		return tag(pokemon).getInt(MOJADO) > 0 ? 1 : 0;
	}

	public static BathState getState(Pokemon pokemon) {
		return BathState.fromId(tag(pokemon).getString(STATE));
	}

	public static void setSuciedad(Pokemon pokemon, int value) {
		tag(pokemon).putInt(SUCIEDAD, clamp(value, 0, MAX_SUCIEDAD));
		syncDirtAspect(pokemon);
		syncState(pokemon);
	}

	public static void setJabonoso(Pokemon pokemon, int value) {
		tag(pokemon).putInt(JABONOSO, clamp(value, 0, MAX_JABONOSO));
		syncJabonosoAspect(pokemon);
		syncState(pokemon);
	}

	public static void setMojado(Pokemon pokemon, int value) {
		tag(pokemon).putInt(MOJADO, value > 0 ? 1 : 0);
		syncState(pokemon);
	}

	/**
	 * Guarda la suciedad actual para el cálculo de amistad al terminar el baño,
	 * solo si aún no hay snapshot (primer enjabonado del ciclo).
	 */
	public static void snapshotSuciedadForBath(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		if (data.contains(SUCIEDAD_BANO)) {
			return;
		}
		int suciedad = getSuciedad(pokemon);
		if (suciedad > 0) {
			data.putInt(SUCIEDAD_BANO, suciedad);
		}
	}

	public static int getSuciedadForFriendship(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		if (data.contains(SUCIEDAD_BANO)) {
			return clamp(data.getInt(SUCIEDAD_BANO), 0, MAX_SUCIEDAD);
		}
		return getSuciedad(pokemon);
	}

	public static boolean addSuciedadFromBattle(Pokemon pokemon, UUID battleId) {
		if (battleId == null || pokemon.getOwnerUUID() == null) {
			return false;
		}
		String last = tag(pokemon).getString(LAST_DIRT_BATTLE);
		if (battleId.toString().equals(last)) {
			return false;
		}
		int before = getSuciedad(pokemon);
		if (before >= MAX_SUCIEDAD) {
			tag(pokemon).putString(LAST_DIRT_BATTLE, battleId.toString());
			return false;
		}
		tag(pokemon).putString(LAST_DIRT_BATTLE, battleId.toString());
		setSuciedad(pokemon, before + 1);
		return true;
	}

	public static void setState(Pokemon pokemon, BathState state) {
		tag(pokemon).putString(STATE, state.name());
	}

	public static void syncState(Pokemon pokemon) {
		int suciedad = getSuciedad(pokemon);
		int jabonoso = getJabonoso(pokemon);
		int mojado = getMojado(pokemon);
		BathState state = getState(pokemon);

		if (suciedad <= 0 && state == BathState.SUCIO) {
			state = BathState.NORMAL;
		}
		if (suciedad > 0 && mojado == 0 && jabonoso == 0 && state == BathState.NORMAL) {
			state = BathState.SUCIO;
		}
		if (jabonoso > 0 && mojado == 0 && (state == BathState.SUCIO || state == BathState.NORMAL)) {
			state = BathState.ENJABONADO;
		}
		if (mojado > 0 && state == BathState.ENJABONADO) {
			state = BathState.MOJADO;
		}
		setState(pokemon, state);
	}

	public static void clearBath(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		data.putInt(SUCIEDAD, 0);
		data.putInt(JABONOSO, 0);
		data.putInt(MOJADO, 0);
		data.remove(SUCIEDAD_BANO);
		data.putString(STATE, BathState.NORMAL.name());
		syncDirtAspect(pokemon);
		syncJabonosoAspect(pokemon);
	}

	public static void syncDirtAspect(Pokemon pokemon) {
		Set<String> aspects = new HashSet<>(pokemon.getForcedAspects());
		boolean hadDirt = aspects.removeIf(aspect -> aspect.startsWith(DIRT_ASPECT_PREFIX));
		int suciedad = getSuciedad(pokemon);
		if (suciedad > 0) {
			aspects.add(DIRT_ASPECT_PREFIX + suciedad);
		} else if (hadDirt || tag(pokemon).contains(SUCIEDAD)) {
			aspects.add(DIRT_ASPECT_PREFIX + "0");
		}
		if (!aspects.equals(pokemon.getForcedAspects())) {
			pokemon.setForcedAspects(aspects);
		}
	}

	public static void syncJabonosoAspect(Pokemon pokemon) {
		Set<String> aspects = new HashSet<>(pokemon.getForcedAspects());
		boolean hadJabonoso = aspects.removeIf(aspect -> aspect.startsWith(JABONOSO_ASPECT_PREFIX));
		int jabonoso = getJabonoso(pokemon);
		if (jabonoso > 0) {
			aspects.add(JABONOSO_ASPECT_PREFIX + jabonoso);
		} else if (hadJabonoso || tag(pokemon).contains(JABONOSO)) {
			aspects.add(JABONOSO_ASPECT_PREFIX + "0");
		}
		if (!aspects.equals(pokemon.getForcedAspects())) {
			pokemon.setForcedAspects(aspects);
		}
	}

	public static void syncBathAspects(Pokemon pokemon) {
		syncDirtAspect(pokemon);
		syncJabonosoAspect(pokemon);
	}

	public static int suciedadFromAspects(Iterable<String> aspects) {
		return levelFromAspects(aspects, DIRT_ASPECT_PREFIX, MAX_SUCIEDAD);
	}

	public static int jabonosoFromAspects(Iterable<String> aspects) {
		return levelFromAspects(aspects, JABONOSO_ASPECT_PREFIX, MAX_JABONOSO);
	}

	private static int levelFromAspects(Iterable<String> aspects, String prefix, int max) {
		if (aspects == null) {
			return -1;
		}
		for (String aspect : aspects) {
			if (aspect != null && aspect.startsWith(prefix)) {
				try {
					return clamp(Integer.parseInt(aspect.substring(prefix.length())), 0, max);
				} catch (NumberFormatException ignored) {
					return -1;
				}
			}
		}
		return -1;
	}

	public static int rollFriendshipFromSuciedad(Pokemon pokemon, RandomSource random) {
		int suciedad = getSuciedadForFriendship(pokemon);
		if (suciedad <= 0) {
			return 0;
		}
		int roll = random.nextInt(FRIENDSHIP_ROLL_MAX - FRIENDSHIP_ROLL_MIN + 1) + FRIENDSHIP_ROLL_MIN;
		return roll * suciedad;
	}

	public static int finishBath(Pokemon pokemon, RandomSource random) {
		int friendship = rollFriendshipFromSuciedad(pokemon, random);
		clearBath(pokemon);
		if (friendship > 0) {
			pokemon.incrementFriendship(friendship, true);
		}
		return friendship;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
