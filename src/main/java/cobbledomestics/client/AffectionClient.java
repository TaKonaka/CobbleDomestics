package cobbledomestics.client;

import java.util.UUID;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionAction;
import cobbledomestics.affection.AffectionData;
import cobbledomestics.affection.network.AffectionActionPacket;
import kotlin.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.interaction.PokemonInteractionGUICreationEvent;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelGUI;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;
import com.cobblemon.mod.common.client.gui.interact.wheel.Orientation;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID, value = Dist.CLIENT)
public final class AffectionClient {
	private static final ResourceLocation CARICIA_ICON = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/caricia.png");
	private static final ResourceLocation ABRAZO_ICON = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/abrazo.png");
	private static boolean cobblemonHooked;

	private AffectionClient() {
	}

	public static void ensureCobblemonHook() {
		if (cobblemonHooked) {
			return;
		}
		cobblemonHooked = true;
		CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION.subscribe(Priority.NORMAL, event -> {
			addAffectionOptions(event);
			return Unit.INSTANCE;
		});
	}

	private static void addAffectionOptions(PokemonInteractionGUICreationEvent event) {
		UUID pokemonId = event.getPokemonID();
		event.addFillingOption(cariciaOption(pokemonId));
		event.addFillingOption(abrazoOption(pokemonId));
	}

	public static void openAffectionOnlyWheel(UUID pokemonEntityId) {
		Multimap<Orientation, InteractWheelOption> options = ArrayListMultimap.create();
		options.put(Orientation.NORTH, cariciaOption(pokemonEntityId));
		options.put(Orientation.NORTHEAST, abrazoOption(pokemonEntityId));
		Minecraft.getInstance().setScreen(new InteractWheelGUI(options, Component.translatable("cobbledomestics.ui.interact.pokemon")));
	}

	private static InteractWheelOption cariciaOption(UUID pokemonEntityId) {
		return new InteractWheelOption(
				CARICIA_ICON,
				null,
				true,
				"cobbledomestics.ui.interact.caricia",
				() -> null,
				() -> {
					sendAction(pokemonEntityId, AffectionAction.CARICIA);
					return Unit.INSTANCE;
				});
	}

	private static InteractWheelOption abrazoOption(UUID pokemonEntityId) {
		return new InteractWheelOption(
				ABRAZO_ICON,
				null,
				true,
				"cobbledomestics.ui.interact.abrazo",
				() -> null,
				() -> {
					sendAction(pokemonEntityId, AffectionAction.ABRAZO);
					return Unit.INSTANCE;
				});
	}

	private static void sendAction(UUID pokemonEntityId, AffectionAction action) {
		PacketDistributor.sendToServer(new AffectionActionPacket(pokemonEntityId, action));
		Minecraft.getInstance().setScreen(null);
	}

	@SubscribeEvent
	public static void onClientStart(ClientPlayerNetworkEvent.LoggingIn event) {
		ensureCobblemonHook();
	}

	/**
	 * Cobblemon only opens the native wheel for the owner. For wild / non-owned Pokémon,
	 * open an affection-only InteractWheel on Shift + right-click.
	 */
	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!event.getLevel().isClientSide()) {
			return;
		}
		if (event.getHand() != InteractionHand.MAIN_HAND) {
			return;
		}
		if (!event.getEntity().isShiftKeyDown()) {
			return;
		}
		if (!(event.getTarget() instanceof PokemonEntity pokemonEntity)) {
			return;
		}
		Pokemon pokemon = pokemonEntity.getPokemon();
		if (AffectionData.isOwnedBy(pokemon, event.getEntity().getUUID())) {
			return;
		}
		ensureCobblemonHook();
		openAffectionOnlyWheel(pokemonEntity.getUUID());
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
	}
}
