package cobbledomestics.affection.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.ImmersiveFocusHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: immersive care screen opened; hold Pokémon attention on this player. */
public record ImmersiveFocusStartPacket(UUID pokemonEntityId) implements CustomPacketPayload {
	public static final Type<ImmersiveFocusStartPacket> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "immersive_focus_start"));

	public static final StreamCodec<FriendlyByteBuf, ImmersiveFocusStartPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			ImmersiveFocusStartPacket::pokemonEntityId,
			ImmersiveFocusStartPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ImmersiveFocusStartPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			ImmersiveFocusHandler.start(player, packet.pokemonEntityId());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, ImmersiveFocusStartPacket::handle);
	}
}
