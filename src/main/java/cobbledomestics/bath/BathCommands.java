package cobbledomestics.bath;

import com.cobblemon.mod.common.api.pokemon.status.Statuses;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionData;
import cobbledomestics.animation.InteractionAnimations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class BathCommands {
	private BathCommands() {
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(buildRoot("cobbledomestics"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
		return Commands.literal(name)
				.requires(source -> source.getEntity() instanceof Player player && player.isCreative())
				.then(Commands.literal("suciedad")
						.then(Commands.argument("valor", IntegerArgumentType.integer(0, BathData.MAX_SUCIEDAD))
								.executes(BathCommands::setSuciedad)))
				.then(Commands.literal("estado")
						.executes(BathCommands::showState))
				.then(Commands.literal("HumorReset")
						.executes(BathCommands::resetHumor))
				.then(Commands.literal("ConfianzaSet")
						.then(Commands.argument("valor", IntegerArgumentType.integer(0, Integer.MAX_VALUE / 4))
								.executes(BathCommands::setConfianza)))
				.then(Commands.literal("Animation")
						.then(Commands.argument("animaciones", StringArgumentType.greedyString())
								.executes(BathCommands::playAnimation)))
				.then(Commands.literal("problema")
						.then(Commands.literal("envenenamiento")
								.executes(ctx -> applyProblem(ctx, Statuses.POISON, "envenenamiento")))
						.then(Commands.literal("paralisis")
								.executes(ctx -> applyProblem(ctx, Statuses.PARALYSIS, "paralisis")))
						.then(Commands.literal("dormido")
								.executes(ctx -> applyProblem(ctx, Statuses.SLEEP, "dormido"))));
	}

	private static int applyProblem(CommandContext<CommandSourceStack> context, PersistentStatus status, String key) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		Pokemon pokemon = target.getPokemon();
		pokemon.applyStatus(status);
		context.getSource().sendSuccess(() -> Component.translatable(
				"message.cobbledomestics.problema.applied",
				pokemon.getDisplayName(true),
				Component.translatable("message.cobbledomestics.problema." + key)), true);
		return 1;
	}

	private static int playAnimation(CommandContext<CommandSourceStack> context) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		String raw = StringArgumentType.getString(context, "animaciones").trim();
		String[] names = raw.split("\\s+");
		if (raw.isEmpty() || names.length == 0 || names[0].isBlank()) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.animation.empty"));
			return 0;
		}
		InteractionAnimations.playOnPokemon(target, names);
		String joined = String.join(" ", names);
		context.getSource().sendSuccess(() -> Component.translatable(
				"message.cobbledomestics.animation.played",
				target.getPokemon().getDisplayName(true),
				joined), true);
		return 1;
	}

	private static int setSuciedad(CommandContext<CommandSourceStack> context) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		int value = IntegerArgumentType.getInteger(context, "valor");
		BathData.setSuciedad(target.getPokemon(), value);
		if (value > 0) {
			BathData.setJabonoso(target.getPokemon(), 0);
			BathData.setMojado(target.getPokemon(), 0);
			BathData.setState(target.getPokemon(), BathState.SUCIO);
		} else {
			BathData.clearBath(target.getPokemon());
		}
		context.getSource().sendSuccess(() -> Component.translatable("message.cobbledomestics.bath.set_dirt", target.getPokemon().getDisplayName(true), value), true);
		return 1;
	}

	private static int showState(CommandContext<CommandSourceStack> context) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		Pokemon pokemon = target.getPokemon();
		BathData.syncState(pokemon);
		int humor = AffectionData.getHumor(pokemon);
		int confianza = AffectionData.getConfianza(pokemon);
		context.getSource().sendSuccess(() -> Component.translatable(
				"message.cobbledomestics.bath.status",
				pokemon.getDisplayName(true),
				BathData.getState(pokemon).name(),
				BathData.getSuciedad(pokemon),
				BathData.getJabonoso(pokemon),
				BathData.getMojado(pokemon),
				humor,
				confianza), false);
		return 1;
	}

	private static int resetHumor(CommandContext<CommandSourceStack> context) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		Pokemon pokemon = target.getPokemon();
		AffectionData.setHumor(pokemon, AffectionData.DEFAULT_HUMOR);
		context.getSource().sendSuccess(() -> Component.translatable(
				"message.cobbledomestics.affection.humor_reset",
				pokemon.getDisplayName(true),
				AffectionData.DEFAULT_HUMOR), true);
		return 1;
	}

	private static int setConfianza(CommandContext<CommandSourceStack> context) {
		PokemonEntity target = findLookedPokemon(context.getSource());
		if (target == null) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.bath.no_target"));
			return 0;
		}
		Pokemon pokemon = target.getPokemon();
		if (!AffectionData.isWild(pokemon)) {
			context.getSource().sendFailure(Component.translatable("message.cobbledomestics.affection.confianza_not_wild", pokemon.getDisplayName(true)));
			return 0;
		}
		int value = IntegerArgumentType.getInteger(context, "valor");
		AffectionData.setConfianza(pokemon, value);
		context.getSource().sendSuccess(() -> Component.translatable(
				"message.cobbledomestics.affection.confianza_set",
				pokemon.getDisplayName(true),
				AffectionData.getConfianza(pokemon)), true);
		return 1;
	}

	private static PokemonEntity findLookedPokemon(CommandSourceStack source) {
		Entity entity = source.getEntity();
		if (entity == null) {
			return null;
		}
		Vec3 look = entity.getLookAngle().scale(8);
		AABB box = entity.getBoundingBox().expandTowards(look).inflate(1.0);
		PokemonEntity closest = null;
		double closestDist = Double.MAX_VALUE;
		for (PokemonEntity pokemonEntity : entity.level().getEntitiesOfClass(PokemonEntity.class, box)) {
			double dist = pokemonEntity.distanceToSqr(entity);
			if (dist < closestDist) {
				closest = pokemonEntity;
				closestDist = dist;
			}
		}
		if (closest != null) {
			return closest;
		}
		if (entity instanceof net.minecraft.world.entity.player.Player player && player.getVehicle() instanceof PokemonEntity riding) {
			return riding;
		}
		return null;
	}
}
