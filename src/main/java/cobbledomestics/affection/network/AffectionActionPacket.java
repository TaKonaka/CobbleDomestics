package cobbledomestics.affection.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionAction;
import cobbledomestics.affection.AffectionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AffectionActionPacket(UUID pokemonEntityId, AffectionAction action) implements CustomPacketPayload {
	public static final Type<AffectionActionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "affection_action"));

	public static final StreamCodec<FriendlyByteBuf, AffectionActionPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			AffectionActionPacket::pokemonEntityId,
			StreamCodec.of(
					(buf, value) -> buf.writeEnum(value),
					buf -> buf.readEnum(AffectionAction.class)),
			AffectionActionPacket::action,
			AffectionActionPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(AffectionActionPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			AffectionHandler.handleAction(player, packet.pokemonEntityId(), packet.action());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, AffectionActionPacket::handle);
	}
}
