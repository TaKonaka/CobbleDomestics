package cobbledomestics.affection.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.JoinTeamHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: player accepted or rejected the join offer.
 */
public record JoinOfferResponsePacket(UUID pokemonEntityId, boolean accepted) implements CustomPacketPayload {
	public static final Type<JoinOfferResponsePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "join_offer_response"));

	public static final StreamCodec<FriendlyByteBuf, JoinOfferResponsePacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			JoinOfferResponsePacket::pokemonEntityId,
			ByteBufCodecs.BOOL,
			JoinOfferResponsePacket::accepted,
			JoinOfferResponsePacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(JoinOfferResponsePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			if (packet.accepted()) {
				JoinTeamHandler.accept(player, packet.pokemonEntityId());
			} else {
				JoinTeamHandler.reject(player, packet.pokemonEntityId());
			}
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, JoinOfferResponsePacket::handle);
	}
}
