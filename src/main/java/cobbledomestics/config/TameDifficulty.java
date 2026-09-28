package cobbledomestics.config;

/**
 * Server tame difficulty: higher divisor → lower trust cap → easier to befriend.
 */
public enum TameDifficulty {
	EASY(3),
	NORMAL(2),
	HARD(1);

	private final int divisor;

	TameDifficulty(int divisor) {
		this.divisor = divisor;
	}

	public int getDivisor() {
		return divisor;
	}
}
