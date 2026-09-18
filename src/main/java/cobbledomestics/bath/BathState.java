package cobbledomestics.bath;

public enum BathState {
	NORMAL,
	SUCIO,
	ENJABONADO,
	MOJADO;

	public static BathState fromId(String id) {
		if (id == null || id.isEmpty()) {
			return NORMAL;
		}
		try {
			return BathState.valueOf(id);
		} catch (IllegalArgumentException ignored) {
			return NORMAL;
		}
	}
}
