package cobbledomestics.config.network;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.config.CobbleDomesticsMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: client's preference for gameplay chat messages. */
public record ShowMessagesPrefPacket(boolean show) implements CustomPacketPayload {
	public static final Type<ShowMessagesPrefPacket> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "show_messages_pref"));

	public static final StreamCodec<FriendlyByteBuf, ShowMessagesPrefPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			ShowMessagesPrefPacket::show,
			ShowMessagesPrefPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ShowMessagesPrefPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			CobbleDomesticsMessages.setShowMessages(player.getUUID(), packet.show());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, ShowMessagesPrefPacket::handle);
	}
}
