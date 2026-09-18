package cobbledomestics.client;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.client.particle.SoapBubbleParticle;
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
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			AffectionClient.ensureCobblemonHook();
			cobbledomestics.affection.network.JoinOfferPacket.CLIENT_OPEN = JoinOfferScreen::open;
		});
	}
}
