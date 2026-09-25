package cobbledomestics.bath.network;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.bath.BathHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: stop scrubbing / clear immersive scrub progress. */
public record BathScrubEndPacket() implements CustomPacketPayload {
	public static final Type<BathScrubEndPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "bath_scrub_end"));

	public static final StreamCodec<FriendlyByteBuf, BathScrubEndPacket> STREAM_CODEC = StreamCodec.unit(new BathScrubEndPacket());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BathScrubEndPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			BathHandler.handleScrubEnd(player);
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, BathScrubEndPacket::handle);
	}
}
