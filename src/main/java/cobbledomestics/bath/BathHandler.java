package cobbledomestics.bath;

import java.util.UUID;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.animation.InteractionAnimations;
import cobbledomestics.init.CobbleDomesticsModItems;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import kotlin.Unit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID)
public final class BathHandler {
	private static final int MOJADO_PARTICLE_INTERVAL = 5;

	private BathHandler() {
	}

	public static void registerCobblemonEvents() {
		CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
			dirtyBattleParticipants(event.getBattle());
			return Unit.INSTANCE;
		});
		CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, event -> {
			dirtyBattleParticipants(event.getBattle());
			return Unit.INSTANCE;
		});
	}

	private static void dirtyBattleParticipants(PokemonBattle battle) {
		UUID battleId = battle.getBattleId();
		for (BattleActor actor : battle.getActors()) {
			if (actor.getType() != ActorType.PLAYER) {
				continue;
			}
			for (BattlePokemon battlePokemon : actor.getPokemonList()) {
				if (battlePokemon.getFacedOpponents().isEmpty()) {
					continue;
				}
				BathData.addSuciedadFromBattle(battlePokemon.getOriginalPokemon(), battleId);
			}
		}
	}

	@SubscribeEvent
	public static void onPokemonJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		if (event.getEntity() instanceof PokemonEntity pokemonEntity) {
			BathData.syncBathAspects(pokemonEntity.getPokemon());
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (event.getServer().getTickCount() % MOJADO_PARTICLE_INTERVAL != 0) {
			return;
		}
		for (ServerLevel level : event.getServer().getAllLevels()) {
			for (Entity entity : level.getEntities().getAll()) {
				if (!(entity instanceof PokemonEntity pokemonEntity)) {
					continue;
				}
				if (BathData.getMojado(pokemonEntity.getPokemon()) <= 0) {
					continue;
				}
				level.sendParticles(
						ParticleTypes.FALLING_WATER,
						pokemonEntity.getX(),
						pokemonEntity.getY() + pokemonEntity.getBbHeight() * 0.75,
						pokemonEntity.getZ(),
						3, 0.3, 0.2, 0.3, 0.01);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof PokemonEntity pokemonEntity)) {
			return;
		}
		Player player = event.getEntity();
		if (player.isShiftKeyDown()) {
			return;
		}
		ItemStack stack = event.getItemStack();
		InteractionResult result = tryBath(player, pokemonEntity, stack, event.getHand());
		if (result.consumesAction()) {
			event.setCanceled(true);
			event.setCancellationResult(result);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onAttackPokemon(AttackEntityEvent event) {
		if (!(event.getTarget() instanceof PokemonEntity)) {
			return;
		}
		ItemStack stack = event.getEntity().getMainHandItem();
		if (isBathTool(stack)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof PokemonEntity)) {
			return;
		}
		if (!(event.getSource().getEntity() instanceof Player player)) {
			return;
		}
		if (isBathTool(player.getMainHandItem())) {
			event.setCanceled(true);
		}
	}

	public static InteractionResult tryBath(Player player, PokemonEntity pokemonEntity, ItemStack stack, InteractionHand hand) {
		boolean soap = stack.is(CobbleDomesticsModItems.SOAP.get());
		boolean water = stack.is(Items.WATER_BUCKET);
		boolean towel = stack.is(CobbleDomesticsModItems.TOALLA.get());
		if (!soap && !water && !towel) {
			return InteractionResult.PASS;
		}

		Pokemon pokemon = pokemonEntity.getPokemon();
		BathData.syncState(pokemon);
		if (!canCareFor(player, pokemon)) {
			tell(player, Component.translatable("message.cobbledomestics.bath.not_owner"));
			return InteractionResult.FAIL;
		}

		if (soap) {
			return useSoap(player, pokemonEntity, pokemon, stack, hand);
		}
		if (water) {
			return useWater(player, pokemonEntity, pokemon, stack, hand);
		}
		return useTowel(player, pokemonEntity, pokemon, stack, hand);
	}

	private static InteractionResult useSoap(Player player, PokemonEntity pokemonEntity, Pokemon pokemon, ItemStack stack, InteractionHand hand) {
		if (player.level().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		BathState state = BathData.getState(pokemon);
		int jabonoso = BathData.getJabonoso(pokemon);
		int suciedad = BathData.getSuciedad(pokemon);

		if (state == BathState.MOJADO || jabonoso >= BathData.MAX_JABONOSO) {
			tell(player, Component.translatable("message.cobbledomestics.bath.already_soapy", name(pokemon)));
			return InteractionResult.FAIL;
		}
		if (jabonoso <= 0 && suciedad <= 0) {
			tell(player, Component.translatable("message.cobbledomestics.bath.not_dirty", name(pokemon)));
			return InteractionResult.FAIL;
		}

		if (suciedad > 0) {
			BathData.snapshotSuciedadForBath(pokemon);
			BathData.setSuciedad(pokemon, 0);
		}
		int next = jabonoso + 1;
		BathData.setJabonoso(pokemon, next);
		BathData.setState(pokemon, BathState.ENJABONADO);
		if (next >= BathData.MAX_JABONOSO) {
			tell(player, Component.translatable("message.cobbledomestics.bath.soaped", name(pokemon)));
		} else {
			tell(player, Component.translatable("message.cobbledomestics.bath.soaping", name(pokemon), String.valueOf(next), String.valueOf(BathData.MAX_JABONOSO)));
		}

		hurtHeldItem(player, stack, hand, BathData.SOAP_COST);
		playAround(pokemonEntity, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, CobbleDomesticsModParticleTypes.SOAP_BUBBLE.get());
		return InteractionResult.CONSUME;
	}

	private static InteractionResult useWater(Player player, PokemonEntity pokemonEntity, Pokemon pokemon, ItemStack stack, InteractionHand hand) {
		if (player.level().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (BathData.getJabonoso(pokemon) < BathData.MAX_JABONOSO) {
			tell(player, Component.translatable("message.cobbledomestics.bath.need_soap", name(pokemon)));
			return InteractionResult.FAIL;
		}

		BathData.setJabonoso(pokemon, 0);
		BathData.setMojado(pokemon, 1);
		BathData.setState(pokemon, BathState.MOJADO);
		if (!player.getAbilities().instabuild) {
			ItemStack empty = new ItemStack(Items.BUCKET);
			if (stack.getCount() == 1) {
				player.setItemInHand(hand, empty);
			} else {
				stack.shrink(1);
				if (!player.getInventory().add(empty)) {
					player.drop(empty, false);
				}
			}
		}
		tell(player, Component.translatable("message.cobbledomestics.bath.wet", name(pokemon)));
		playAround(pokemonEntity, SoundEvents.BUCKET_EMPTY, ParticleTypes.FALLING_WATER);
		return InteractionResult.CONSUME;
	}

	private static InteractionResult useTowel(Player player, PokemonEntity pokemonEntity, Pokemon pokemon, ItemStack stack, InteractionHand hand) {
		if (player.level().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (BathData.getState(pokemon) != BathState.MOJADO) {
			tell(player, Component.translatable("message.cobbledomestics.bath.need_wet", name(pokemon)));
			return InteractionResult.FAIL;
		}

		int friendship = BathData.finishBath(pokemon, player.getRandom());
		hurtHeldItem(player, stack, hand, 1);
		tell(player, Component.translatable("message.cobbledomestics.bath.clean", name(pokemon), String.valueOf(friendship)));
		playAround(pokemonEntity, SoundEvents.WOOL_PLACE, ParticleTypes.CLOUD);
		InteractionAnimations.playOnPokemon(pokemonEntity, "cry");
		return InteractionResult.CONSUME;
	}

	private static void hurtHeldItem(Player player, ItemStack stack, InteractionHand hand, int amount) {
		if (player.getAbilities().instabuild) {
			return;
		}
		stack.hurtAndBreak(amount, player, LivingEntity.getSlotForHand(hand));
	}

	private static void playAround(PokemonEntity entity, net.minecraft.sounds.SoundEvent sound, net.minecraft.core.particles.ParticleOptions particle) {
		Level level = entity.level();
		level.playSound(null, entity.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, 1.1F);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 16, 0.45, 0.35, 0.45, 0.02);
		}
	}

	private static void tell(Player player, Component message) {
		if (!player.level().isClientSide) {
			player.displayClientMessage(message, true);
		}
	}

	private static Component name(Pokemon pokemon) {
		return pokemon.getDisplayName(true);
	}

	private static boolean canCareFor(Player player, Pokemon pokemon) {
		UUID owner = pokemon.getOwnerUUID();
		return owner != null && owner.equals(player.getUUID());
	}

	private static boolean isBathTool(ItemStack stack) {
		return stack.is(CobbleDomesticsModItems.SOAP.get()) || stack.is(CobbleDomesticsModItems.TOALLA.get());
	}
}
