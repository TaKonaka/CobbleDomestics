package cobbledomestics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CobbleDomesticsConfig {
	public static final ModConfigSpec SERVER_SPEC;
	public static final ModConfigSpec CLIENT_SPEC;

	public static final ModConfigSpec.EnumValue<TameDifficulty> TAME_DIFFICULTY;
	public static final ModConfigSpec.BooleanValue SHOW_GAMEPLAY_MESSAGES;

	static {
		ModConfigSpec.Builder server = new ModConfigSpec.Builder();
		TAME_DIFFICULTY = server
				.comment(
						"Trust cap divisor for wild Pokémon: LvCaptura = round((level × maxFullness) / divisor).",
						"EASY=3 (easier), NORMAL=2, HARD=1 (harder).")
				.translation("config.cobbledomestics.tameDifficulty")
				.defineEnum("tameDifficulty", TameDifficulty.NORMAL);
		SERVER_SPEC = server.build();

		ModConfigSpec.Builder client = new ModConfigSpec.Builder();
		SHOW_GAMEPLAY_MESSAGES = client
				.comment("Show CobbleDomestics gameplay messages in chat (bath, beans, join errors). Remap immersive mode in Controls.")
				.translation("config.cobbledomestics.showGameplayMessages")
				.define("showGameplayMessages", true);
		CLIENT_SPEC = client.build();
	}

	private CobbleDomesticsConfig() {
	}

	public static TameDifficulty getTameDifficulty() {
		return TAME_DIFFICULTY.get();
	}

	public static int getTameDivisor() {
		return getTameDifficulty().getDivisor();
	}

	public static boolean showGameplayMessages() {
		return SHOW_GAMEPLAY_MESSAGES.get();
	}
}
