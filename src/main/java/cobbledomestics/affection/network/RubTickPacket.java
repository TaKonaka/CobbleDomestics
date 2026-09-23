package cobbledomestics.affection.network;

import java.util.UUID;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RubTickPacket(UUID pokemonEntityId, int rubSpeed) implements CustomPacketPayload {
	public static final Type<RubTickPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "rub_tick"));

	public static final StreamCodec<FriendlyByteBuf, RubTickPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			RubTickPacket::pokemonEntityId,
			ByteBufCodecs.VAR_INT,
			RubTickPacket::rubSpeed,
			RubTickPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(RubTickPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			AffectionHandler.handleRubTick(player, packet.pokemonEntityId(), packet.rubSpeed());
		});
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, RubTickPacket::handle);
	}
}
