package cobbledomestics.affection.network;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: pause petting and show attack hand for 1 second. */
public record RubAttackPacket() implements CustomPacketPayload {
	public static final Type<RubAttackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "rub_attack"));

	public static final StreamCodec<FriendlyByteBuf, RubAttackPacket> STREAM_CODEC = StreamCodec.unit(new RubAttackPacket());

	/** Set from client bootstrap; no-op on dedicated server. */
	public static Runnable CLIENT_APPLY = () -> {
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(RubAttackPacket packet, IPayloadContext context) {
		context.enqueueWork(CLIENT_APPLY);
	}

	public static void register() {
		CobbleDomesticsMod.addNetworkMessage(TYPE, STREAM_CODEC, RubAttackPacket::handle);
	}
}
