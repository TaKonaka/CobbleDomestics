package cobbledomestics.client;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.client.particle.SoapBubbleParticle;
import cobbledomestics.client.particle.StatusStainParticle;
import cobbledomestics.client.particle.TypeNoteParticle;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CobbleDomesticsClient {
	private CobbleDomesticsClient() {
	}

	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.SOAP_BUBBLE.get(), SoapBubbleParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.ENVENE.get(), StatusStainParticle.EnveneProvider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.SHOCK.get(), StatusStainParticle.ShockProvider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.ACERO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.AGUA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.DRAGON.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.ELECTRICO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.FANTASMA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.FUEGO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.HADA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.HIELO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.INSECTO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.LUCHA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.NORMAL.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.PLANTA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.PSIQUICO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.ROCA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.SINIESTRO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.TIERRA.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.VENENO.get(), TypeNoteParticle.Provider::new);
		event.registerSpriteSet(CobbleDomesticsModParticleTypes.VOLADOR.get(), TypeNoteParticle.Provider::new);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			cobbledomestics.affection.network.JoinOfferPacket.CLIENT_OPEN = JoinOfferScreen::open;
			cobbledomestics.affection.network.RubHintPacket.CLIENT_APPLY = AffectionClient::applyRubHint;
			cobbledomestics.affection.network.RubAttackPacket.CLIENT_APPLY = AffectionClient::beginAttackPause;
		});
	}
}
