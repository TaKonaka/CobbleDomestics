package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, CobbleDomesticsMod.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> MASSAGE = REGISTRY.register("massage",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "massage")));

	public static final DeferredHolder<SoundEvent, SoundEvent> GOLPE = REGISTRY.register("golpe",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "golpe")));

	private CobbleDomesticsModSounds() {
	}
}
