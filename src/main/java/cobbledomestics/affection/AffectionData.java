package cobbledomestics.affection;

import java.util.UUID;

import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/**
 * Humor (all Pokémon), Confianza (wild only), and rub gusto prefs on {@code cobbledomestics} persistent NBT.
 */
public final class AffectionData {
	public static final int MAX_HUMOR = 10;
	public static final int DEFAULT_HUMOR = 0;
	public static final int DEFAULT_CONFIANZA = 0;
	/** Humor gained per successful rub (fills toward {@link #MAX_HUMOR}). */
	public static final int RUB_HUMOR_GAIN = 2;
	public static final int RUB_FRIENDSHIP_REWARD = 5;
	public static final int RUB_CONFIANZA_REWARD = 3;
	/** Progress 0–15; gains points every {@link #RUB_PROGRESS_INTERVAL_TICKS} while rubbing. */
	public static final int RUB_PROGRESS_MAX = 15;
	/** World ticks between progress gains (5 ticks ≈ 0.25s). */
	public static final int RUB_PROGRESS_INTERVAL_TICKS = 10;
	/** Points added per interval when rub speed matches the active gusto. */
	public static final int RUB_PROGRESS_MATCH = 3;
	/** Points added per interval when rub speed is outside gusto tolerance. */
	public static final int RUB_PROGRESS_MISMATCH = 1;
	public static final int RUB_SPEED_MIN = 0;
	public static final int RUB_SPEED_MAX = 5;
	/**
	 * Allowed |speed - gusto| for a tick to count as matching rhythm.
	 * Wide on purpose: mouse speed is noisy in immersive free-cursor mode.
	 */
	public static final int RUB_SPEED_TOLERANCE = 3;
	public static final int GUSTO_MIN = 1;
	public static final int GUSTO_MAX = 5;
	/** World ticks between humor decay (−1). */
	public static final int HUMOR_DECAY_INTERVAL = 1200;

	private static final String ROOT = "cobbledomestics";
	private static final String HUMOR = "humor";
	private static final String CONFIANZA = "confianza";
	private static final String GUSTO1 = "gusto1";
	private static final String GUSTO2 = "gusto2";
	private static final String ACTIVE_GUSTO = "activeGusto";

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

	/**
	 * Dynamic join threshold: {@code (level × maxFullness) / 2}, minimum 1.
	 */
	public static int getLvCaptura(Pokemon pokemon) {
		return Math.max(1, (pokemon.getLevel() * pokemon.getMaxFullness()) / 2);
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

	/**
	 * Adds humor toward {@link #MAX_HUMOR}. Returns false if already full.
	 */
	public static boolean tryAddHumor(Pokemon pokemon, int amount) {
		int current = getHumor(pokemon);
		if (current >= MAX_HUMOR) {
			return false;
		}
		setHumor(pokemon, current + Math.max(0, amount));
		return true;
	}

	/** Decays humor by 1 when above 0. */
	public static void decayHumor(Pokemon pokemon) {
		int current = getHumor(pokemon);
		if (current > 0) {
			setHumor(pokemon, current - 1);
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
		return clamp(data.getInt(CONFIANZA), 0, getLvCaptura(pokemon));
	}

	public static void setConfianza(Pokemon pokemon, int value) {
		if (!isWild(pokemon)) {
			return;
		}
		tag(pokemon).putInt(CONFIANZA, clamp(value, 0, getLvCaptura(pokemon)));
	}

	public static void addConfianza(Pokemon pokemon, int amount) {
		if (!isWild(pokemon) || amount == 0) {
			return;
		}
		setConfianza(pokemon, getConfianza(pokemon) + amount);
	}

	public static boolean hasGustos(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		return data.contains(GUSTO1) && data.contains(GUSTO2)
				&& data.getInt(GUSTO1) >= GUSTO_MIN && data.getInt(GUSTO2) >= GUSTO_MIN;
	}

	/**
	 * Assigns gusto1/gusto2 (1–5, distinct) on first interaction.
	 */
	public static void ensureGustos(Pokemon pokemon, RandomSource random) {
		if (hasGustos(pokemon)) {
			return;
		}
		rollGustos(pokemon, random);
		tag(pokemon).putInt(ACTIVE_GUSTO, 1);
	}

	public static int getActiveGustoSlot(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		if (!data.contains(ACTIVE_GUSTO)) {
			return 1;
		}
		int slot = data.getInt(ACTIVE_GUSTO);
		return slot == 2 ? 2 : 1;
	}

	public static int getActiveGustoSpeed(Pokemon pokemon) {
		CompoundTag data = tag(pokemon);
		int slot = getActiveGustoSlot(pokemon);
		int speed = slot == 2 ? data.getInt(GUSTO2) : data.getInt(GUSTO1);
		return clamp(speed, GUSTO_MIN, GUSTO_MAX);
	}

	/** True when rub speed matches the active gusto within {@link #RUB_SPEED_TOLERANCE}. */
	public static boolean matchesGustoSpeed(int rubSpeed, int requiredGusto) {
		return Math.abs(rubSpeed - requiredGusto) <= RUB_SPEED_TOLERANCE;
	}

	public static RubHint getRubHint(int rubSpeed, int requiredGusto) {
		if (matchesGustoSpeed(rubSpeed, requiredGusto)) {
			return RubHint.OK;
		}
		return rubSpeed < requiredGusto ? RubHint.TOO_SLOW : RubHint.TOO_FAST;
	}

	/**
	 * After a successful rub: slot 1 → require slot 2; slot 2 → re-roll both and require slot 1.
	 */
	public static void advanceGustoAfterSuccess(Pokemon pokemon, RandomSource random) {
		if (getActiveGustoSlot(pokemon) == 1) {
			tag(pokemon).putInt(ACTIVE_GUSTO, 2);
			return;
		}
		rollGustos(pokemon, random);
		tag(pokemon).putInt(ACTIVE_GUSTO, 1);
	}

	public static boolean isOwnedBy(Pokemon pokemon, UUID playerId) {
		UUID owner = pokemon.getOwnerUUID();
		return owner != null && owner.equals(playerId);
	}

	private static void rollGustos(Pokemon pokemon, RandomSource random) {
		int g1 = random.nextInt(GUSTO_MAX - GUSTO_MIN + 1) + GUSTO_MIN;
		int g2 = random.nextInt(GUSTO_MAX - GUSTO_MIN + 1) + GUSTO_MIN;
		while (g2 == g1) {
			g2 = random.nextInt(GUSTO_MAX - GUSTO_MIN + 1) + GUSTO_MIN;
		}
		CompoundTag data = tag(pokemon);
		data.putInt(GUSTO1, g1);
		data.putInt(GUSTO2, g2);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
