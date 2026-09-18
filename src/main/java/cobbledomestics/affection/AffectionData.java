package cobbledomestics.affection;

import java.util.UUID;

import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/**
 * Humor (all Pokémon) and Confianza (wild only) on {@code cobbledomestics} persistent NBT.
 */
public final class AffectionData {
	public static final int MAX_HUMOR = 10;
	public static final int DEFAULT_HUMOR = 10;
	public static final int MAX_CONFIANZA = 50;
	public static final int DEFAULT_CONFIANZA = 0;
	public static final int CARICIA_HUMOR_COST = 3;
	public static final int ABRAZO_HUMOR_COST = 4;
	public static final int ABRAZO_CONFIANZA_REQUIRED = 25;
	public static final int CARICIA_REWARD_MIN = 2;
	public static final int CARICIA_REWARD_MAX = 5;
	public static final int ABRAZO_REWARD_MIN = 5;
	public static final int ABRAZO_REWARD_MAX = 15;
	public static final int HUMOR_REGEN_INTERVAL = 1200;

	private static final String ROOT = "cobbledomestics";
	private static final String HUMOR = "humor";
	private static final String CONFIANZA = "confianza";

	private AffectionData() {
	}

	public static CompoundTag tag(Pokemon pokemon) {
		CompoundTag persistent = pokemon.getPersistentData();
		CompoundTag data = persistent.getCompound(ROOT);
		if (!persistent.contains(ROOT)) {
			persistent.put(ROOT, data);
		}
		return data;
	}

	public static boolean isWild(Pokemon pokemon) {
		return pokemon.getOwnerUUID() == null;
	}

	public static int getHumor(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		if (!data.contains(HUMOR)) {
			return DEFAULT_HUMOR;
		}
		return clamp(data.getInt(HUMOR), 0, MAX_HUMOR);
	}

	public static void setHumor(Pokemon pokemon, int value) {
		tag(pokemon).putInt(HUMOR, clamp(value, 0, MAX_HUMOR));
	}

	public static boolean trySpendHumor(Pokemon pokemon, int cost) {
		int current = getHumor(pokemon);
		if (current < cost) {
			return false;
		}
		setHumor(pokemon, current - cost);
		return true;
	}

	public static void regenHumor(Pokemon pokemon) {
		int current = getHumor(pokemon);
		if (current < MAX_HUMOR) {
			setHumor(pokemon, current + 1);
		}
	}

	public static int getConfianza(Pokemon pokemon) {
		if (!isWild(pokemon)) {
			return 0;
		}
		CompoundTag data = tag(pokemon);
		if (!data.contains(CONFIANZA)) {
			return DEFAULT_CONFIANZA;
		}
		return clamp(data.getInt(CONFIANZA), 0, MAX_CONFIANZA);
	}

	public static void setConfianza(Pokemon pokemon, int value) {
		if (!isWild(pokemon)) {
			return;
		}
		tag(pokemon).putInt(CONFIANZA, clamp(value, 0, MAX_CONFIANZA));
	}

	public static void addConfianza(Pokemon pokemon, int amount) {
		if (!isWild(pokemon) || amount == 0) {
			return;
		}
		setConfianza(pokemon, getConfianza(pokemon) + amount);
	}

	public static int rollCariciaReward(RandomSource random) {
		return random.nextInt(CARICIA_REWARD_MAX - CARICIA_REWARD_MIN + 1) + CARICIA_REWARD_MIN;
	}

	public static int rollAbrazoReward(RandomSource random) {
		return random.nextInt(ABRAZO_REWARD_MAX - ABRAZO_REWARD_MIN + 1) + ABRAZO_REWARD_MIN;
	}

	public static boolean canAbrazoConfianza(Pokemon pokemon) {
		return !isWild(pokemon) || getConfianza(pokemon) > ABRAZO_CONFIANZA_REQUIRED;
	}

	public static boolean isOwnedBy(Pokemon pokemon, UUID playerId) {
		UUID owner = pokemon.getOwnerUUID();
		return owner != null && owner.equals(playerId);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
