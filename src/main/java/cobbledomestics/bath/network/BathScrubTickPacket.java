package cobbledomestics.bath.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.bath.BathHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: one tick of scrubbing with soap or towel; scrubSpeed scales apply time. */
public record BathScrubTickPacket(UUID pokemonEntityId, int scrubSpeed) implements CustomPacketPayload {
	public static final Type<BathScrubTickPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "bath_scrub_tick"));

	public static final StreamCodec<FriendlyByteBuf, BathScrubTickPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			BathScrubTickPacket::pokemonEntityId,
			ByteBufCodecs.VAR_INT,
			BathScrubTickPacket::scrubSpeed,
			BathScrubTickPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BathScrubTickPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			BathHandler.handleScrubTick(player, packet.pokemonEntityId(), packet.scrubSpeed());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, BathScrubTickPacket::handle);
	}
}
