package cobbledomestics.bath.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.bath.BathHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: rinse with water bucket while immersive bathing. */
public record BathRinsePacket(UUID pokemonEntityId) implements CustomPacketPayload {
	public static final Type<BathRinsePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "bath_rinse"));

	public static final StreamCodec<FriendlyByteBuf, BathRinsePacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			BathRinsePacket::pokemonEntityId,
			BathRinsePacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BathRinsePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			BathHandler.handleRinse(player, packet.pokemonEntityId());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, BathRinsePacket::handle);
	}
}
