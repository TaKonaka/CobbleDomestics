package cobbledomestics.client;

import cobbledomestics.bath.BathItems;
import cobbledomestics.init.CobbleDomesticsModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Looping scrub sound while holding LMB with soap, towel, or pipette in immersive mode. */
public final class ImmersiveScrubSound extends AbstractTickableSoundInstance {
	private final Player player;
	private boolean stopped;

	public ImmersiveScrubSound(Player player, SoundEvent sound) {
		super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.player = player;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.55F;
		this.pitch = 1.0F;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	public static ImmersiveScrubSound forHeldItem(Player player, ItemStack held) {
		if (BathItems.isSoap(held)) {
			return new ImmersiveScrubSound(player, CobbleDomesticsModSounds.SOAP_SCRUB.get());
		}
		if (BathItems.isTowel(held)) {
			return new ImmersiveScrubSound(player, CobbleDomesticsModSounds.TOWEL_SCRUB.get());
		}
		if (BathItems.isPipeta(held)) {
			return new ImmersiveScrubSound(player, CobbleDomesticsModSounds.PIPETA.get());
		}
		return null;
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
