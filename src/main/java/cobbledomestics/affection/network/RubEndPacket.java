package cobbledomestics.affection.network;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RubEndPacket() implements CustomPacketPayload {
	public static final Type<RubEndPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "rub_end"));

	public static final StreamCodec<FriendlyByteBuf, RubEndPacket> STREAM_CODEC = StreamCodec.unit(new RubEndPacket());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(RubEndPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			AffectionHandler.handleRubEnd(player);
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, RubEndPacket::handle);
	}
}
