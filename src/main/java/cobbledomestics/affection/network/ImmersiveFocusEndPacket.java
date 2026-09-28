package cobbledomestics.affection.network;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.ImmersiveFocusHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: immersive care screen closed; release Pokémon attention. */
public record ImmersiveFocusEndPacket() implements CustomPacketPayload {
	public static final Type<ImmersiveFocusEndPacket> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "immersive_focus_end"));

	public static final StreamCodec<FriendlyByteBuf, ImmersiveFocusEndPacket> STREAM_CODEC =
			StreamCodec.unit(new ImmersiveFocusEndPacket());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ImmersiveFocusEndPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			ImmersiveFocusHandler.end(player);
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, ImmersiveFocusEndPacket::handle);
	}
}
