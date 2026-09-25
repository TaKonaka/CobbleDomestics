package cobbledomestics.client;

import cobbledomestics.affection.RubHint;
import cobbledomestics.init.CobbleDomesticsModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Looping hand SFX while rubbing; switches between correct and incorrect gusto speed. */
public final class ImmersiveRubSound extends AbstractTickableSoundInstance {
	private final Player player;
	private final boolean correct;
	private boolean stopped;

	public ImmersiveRubSound(Player player, boolean correct) {
		super(soundFor(correct), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.player = player;
		this.correct = correct;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.5F;
		this.pitch = 1.0F;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	public boolean isCorrect() {
		return correct;
	}

	public static boolean isCorrectHint(RubHint hint) {
		return hint == null || hint == RubHint.OK;
	}

	private static SoundEvent soundFor(boolean correct) {
		return correct
				? CobbleDomesticsModSounds.HAND_CORRECT.get()
				: CobbleDomesticsModSounds.HAND_INCORRECT.get();
	}

	@Override
	public void tick() {
		if (stopped || player.isRemoved()) {
			this.stop();
			return;
		}
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	public void requestStop() {
		this.stopped = true;
		this.stop();
	}
}
