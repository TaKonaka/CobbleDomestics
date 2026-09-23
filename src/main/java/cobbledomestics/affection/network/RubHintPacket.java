package cobbledomestics.affection.network;

import java.util.function.Consumer;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.RubHint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: tell the client whether to rub slower, faster, or keep pace.
 */
public record RubHintPacket(RubHint hint) implements CustomPacketPayload {
	public static final Type<RubHintPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "rub_hint"));

	public static final StreamCodec<FriendlyByteBuf, RubHintPacket> STREAM_CODEC = StreamCodec.composite(
			StreamCodec.of(
					(buf, value) -> buf.writeEnum(value),
					buf -> buf.readEnum(RubHint.class)),
			RubHintPacket::hint,
			RubHintPacket::new);

	/** Set from client bootstrap; no-op on dedicated server. */
	public static Consumer<RubHint> CLIENT_APPLY = hint -> {
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(RubHintPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> CLIENT_APPLY.accept(packet.hint()));
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, RubHintPacket::handle);
	}
}
