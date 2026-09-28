package cobbledomestics.client;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.client.particle.SoapBubbleParticle;
import cobbledomestics.client.particle.StatusStainParticle;
import cobbledomestics.client.particle.TypeNoteParticle;
import cobbledomestics.config.CobbleDomesticsConfig;
import cobbledomestics.config.CobbleDomesticsMessages;
import cobbledomestics.config.network.ShowMessagesPrefPacket;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

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
			ModList.get().getModContainerById(CobbleDomesticsMod.MODID).ifPresent(CobbleDomesticsClient::registerConfigScreen);
		});
		NeoForge.EVENT_BUS.addListener(CobbleDomesticsClient::onClientLogin);
		NeoForge.EVENT_BUS.addListener(CobbleDomesticsClient::onClientLogout);
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getModId().equals(CobbleDomesticsMod.MODID)
				&& event.getConfig().getSpec() == CobbleDomesticsConfig.CLIENT_SPEC) {
			syncShowMessagesPreference();
		}
	}

	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent.Loading event) {
		if (event.getConfig().getModId().equals(CobbleDomesticsMod.MODID)
				&& event.getConfig().getSpec() == CobbleDomesticsConfig.CLIENT_SPEC) {
			syncShowMessagesPreference();
		}
	}

	private static void registerConfigScreen(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}

	private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		syncShowMessagesPreference();
	}

	private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		if (event.getPlayer() != null) {
			CobbleDomesticsMessages.clear(event.getPlayer().getUUID());
		}
	}

	/** Sends the local client preference to the server when connected. */
	public static void syncShowMessagesPreference() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) {
			return;
		}
		PacketDistributor.sendToServer(new ShowMessagesPrefPacket(CobbleDomesticsConfig.showGameplayMessages()));
	}
}
