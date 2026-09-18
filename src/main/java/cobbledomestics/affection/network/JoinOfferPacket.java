package cobbledomestics.affection.network;

import java.util.UUID;
import java.util.function.BiConsumer;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: open the "wants to join your team" confirm screen.
 */
public record JoinOfferPacket(UUID pokemonEntityId, String pokemonName) implements CustomPacketPayload {
	public static final Type<JoinOfferPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "join_offer"));

	public static final StreamCodec<FriendlyByteBuf, JoinOfferPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			JoinOfferPacket::pokemonEntityId,
			ByteBufCodecs.STRING_UTF8,
			JoinOfferPacket::pokemonName,
			JoinOfferPacket::new);

	/** Set from client bootstrap; no-op on dedicated server. */
	public static BiConsumer<UUID, String> CLIENT_OPEN = (id, name) -> {
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(JoinOfferPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> CLIENT_OPEN.accept(packet.pokemonEntityId(), packet.pokemonName()));
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, JoinOfferPacket::handle);
	}
}
