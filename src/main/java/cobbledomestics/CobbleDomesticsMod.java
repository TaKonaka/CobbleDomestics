package cobbledomestics;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cobbledomestics.affection.network.ImmersiveFocusEndPacket;
import cobbledomestics.affection.network.ImmersiveFocusStartPacket;
import cobbledomestics.affection.network.JoinOfferPacket;
import cobbledomestics.affection.network.JoinOfferResponsePacket;
import cobbledomestics.affection.network.RubAttackPacket;
import cobbledomestics.affection.network.RubEndPacket;
import cobbledomestics.affection.network.RubHintPacket;
import cobbledomestics.affection.network.RubPokePacket;
import cobbledomestics.affection.network.RubTickPacket;
import cobbledomestics.bath.BathHandler;
import cobbledomestics.bath.network.BathRinsePacket;
import cobbledomestics.bath.network.BathScrubEndPacket;
import cobbledomestics.bath.network.BathScrubTickPacket;
import cobbledomestics.config.CobbleDomesticsConfig;
import cobbledomestics.config.network.ShowMessagesPrefPacket;
import cobbledomestics.init.CobbleDomesticsModBlocks;
import cobbledomestics.init.CobbleDomesticsModItems;
import cobbledomestics.init.CobbleDomesticsModRecipes;
import cobbledomestics.init.CobbleDomesticsModSounds;
import cobbledomestics.init.CobbleDomesticsModTabs;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(CobbleDomesticsMod.MODID)
public class CobbleDomesticsMod {
	public static final Logger LOGGER = LogManager.getLogger(CobbleDomesticsMod.class);
	public static final String MODID = "cobbledomestics";

	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public CobbleDomesticsMod(IEventBus modEventBus, ModContainer container) {
		modEventBus.addListener(this::registerNetworking);
		container.registerConfig(ModConfig.Type.SERVER, CobbleDomesticsConfig.SERVER_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, CobbleDomesticsConfig.CLIENT_SPEC);
		CobbleDomesticsModBlocks.REGISTRY.register(modEventBus);
		CobbleDomesticsModItems.REGISTRY.register(modEventBus);
		CobbleDomesticsModRecipes.SERIALIZERS.register(modEventBus);
		CobbleDomesticsModTabs.register(modEventBus);
		CobbleDomesticsModParticleTypes.REGISTRY.register(modEventBus);
		CobbleDomesticsModSounds.REGISTRY.register(modEventBus);
		BathHandler.registerCobblemonEvents();
		RubTickPacket.register();
		RubEndPacket.register();
		RubHintPacket.register();
		RubPokePacket.register();
		RubAttackPacket.register();
		ImmersiveFocusStartPacket.register();
		ImmersiveFocusEndPacket.register();
		JoinOfferPacket.register();
		JoinOfferResponsePacket.register();
		BathScrubTickPacket.register();
		BathScrubEndPacket.register();
		BathRinsePacket.register();
		ShowMessagesPrefPacket.register();
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered) {
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		}
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}
}
