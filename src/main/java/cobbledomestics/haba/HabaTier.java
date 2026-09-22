package cobbledomestics.haba;

public enum HabaTier {
	BASIC(3, 2, 5, 3, 1),
	IRIS(10, 15, 15, 5, 2),
	RAINBOW(80, 50, 0, 0, 3);

	private final int baseAmistad;
	private final int baseConfianza;
	private final int extraAmistad;
	private final int extraConfianza;
	private final int satietyCost;

	HabaTier(int baseAmistad, int baseConfianza, int extraAmistad, int extraConfianza, int satietyCost) {
		this.baseAmistad = baseAmistad;
		this.baseConfianza = baseConfianza;
		this.extraAmistad = extraAmistad;
		this.extraConfianza = extraConfianza;
		this.satietyCost = satietyCost;
	}

	public int baseAmistad() {
		return baseAmistad;
	}

	public int baseConfianza() {
		return baseConfianza;
	}

	public int extraAmistad() {
		return extraAmistad;
	}

	public int extraConfianza() {
		return extraConfianza;
	}

	public int satietyCost() {
		return satietyCost;
	}

	public boolean hasTypeBonus() {
		return this != RAINBOW;
	}

	public String idPrefix() {
		return switch (this) {
			case BASIC -> "basic";
			case IRIS -> "iris";
			case RAINBOW -> "arco_iris";
		};
	}
}
