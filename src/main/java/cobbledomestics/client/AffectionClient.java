package cobbledomestics.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.RubHint;
import cobbledomestics.bath.BathItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Toggle immersive care (mimos / bath) with the remappable action key when looking at a Pokémon.
 */
@EventBusSubscriber(modid = CobbleDomesticsMod.MODID, value = Dist.CLIENT)
public final class AffectionClient {
	private static final double MAX_INTERACT_DISTANCE = 2.0;
	/** Blocks re-entering immersive immediately after exit (mouse-bound keys stay "down"). */
	private static final int REOPEN_COOLDOWN_TICKS = 12;
	private static boolean rubKeyWasDown;
	private static int reopenCooldown;

	private AffectionClient() {
	}

	public static void applyRubHint(RubHint hint) {
		if (Minecraft.getInstance().screen instanceof ImmersiveInteractScreen immersive) {
			immersive.applyRubHint(hint);
		}
	}

	public static void beginAttackPause() {
		if (Minecraft.getInstance().screen instanceof ImmersiveInteractScreen immersive) {
			immersive.beginAttackPause();
		}
	}

	public static void onImmersiveClosed() {
		rubKeyWasDown = true;
		reopenCooldown = REOPEN_COOLDOWN_TICKS;
	}

	public static void consumeRubToggle() {
		rubKeyWasDown = true;
		reopenCooldown = REOPEN_COOLDOWN_TICKS;
	}

	public static boolean isImmersiveOpen() {
		return Minecraft.getInstance().screen instanceof ImmersiveInteractScreen;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			rubKeyWasDown = false;
			reopenCooldown = 0;
			return;
		}

		if (reopenCooldown > 0) {
			reopenCooldown--;
		}

		boolean keyDown = CobbleDomesticsKeyMappings.RUB.isDown();
		boolean pressed = keyDown && !rubKeyWasDown;
		rubKeyWasDown = keyDown;

		if (!pressed) {
			return;
		}

		if (mc.screen instanceof ImmersiveInteractScreen) {
			// Screen handles stop / exit via key/mouse; do not reopen here.
			return;
		}

		if (mc.screen != null) {
			return;
		}

		if (reopenCooldown > 0) {
			return;
		}

		PokemonEntity target = findLookedPokemon(mc);
		if (target == null) {
			return;
		}

		ItemStack held = mc.player.getMainHandItem();
		if (held.isEmpty() || BathItems.isBathInteractItem(held)) {
			rubKeyWasDown = true;
			mc.setScreen(new ImmersiveInteractScreen(target.getUUID()));
		}
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof ImmersiveInteractScreen) {
			mc.setScreen(null);
		}
		rubKeyWasDown = false;
	}

	private static PokemonEntity findLookedPokemon(Minecraft mc) {
		HitResult hit = mc.hitResult;
		if (!(hit instanceof EntityHitResult entityHit) || hit.getType() != HitResult.Type.ENTITY) {
			return null;
		}
		Entity entity = entityHit.getEntity();
		if (!(entity instanceof PokemonEntity pokemonEntity)) {
			return null;
		}
		if (mc.player != null && mc.player.distanceTo(pokemonEntity) > MAX_INTERACT_DISTANCE) {
			return null;
		}
		return pokemonEntity;
	}
}
