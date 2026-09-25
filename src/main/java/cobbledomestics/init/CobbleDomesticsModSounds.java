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

	public static final DeferredHolder<SoundEvent, SoundEvent> RELAX_ENTER = REGISTRY.register("relax_enter",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "relax_enter")));

	public static final DeferredHolder<SoundEvent, SoundEvent> PIPETA = REGISTRY.register("pipeta",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "pipeta")));

	public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE = REGISTRY.register("bubble",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "bubble")));

	public static final DeferredHolder<SoundEvent, SoundEvent> BATH_SUCCESS = REGISTRY.register("bath_success",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "bath_success")));

	public static final DeferredHolder<SoundEvent, SoundEvent> TEAM_JOIN = REGISTRY.register("team_join",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "team_join")));

	public static final DeferredHolder<SoundEvent, SoundEvent> HAND_CORRECT = REGISTRY.register("hand_correct",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "hand_correct")));

	public static final DeferredHolder<SoundEvent, SoundEvent> HAND_INCORRECT = REGISTRY.register("hand_incorrect",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "hand_incorrect")));

	public static final DeferredHolder<SoundEvent, SoundEvent> SOAP_SCRUB = REGISTRY.register("soap_scrub",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "soap_scrub")));

	public static final DeferredHolder<SoundEvent, SoundEvent> TOWEL_SCRUB = REGISTRY.register("towel_scrub",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "towel_scrub")));

	private CobbleDomesticsModSounds() {
	}
}
