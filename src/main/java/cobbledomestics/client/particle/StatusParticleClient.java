package cobbledomestics.client.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cobblemon.mod.common.api.pokemon.status.Statuses;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.particle.CobbleDomesticsModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Spawns entity-anchored poison/paralysis particles for Pokémon with those statuses.
 */
@EventBusSubscriber(modid = CobbleDomesticsMod.MODID, value = Dist.CLIENT)
public final class StatusParticleClient {
	private static final int ENVENE_COOLDOWN = 35;
	private static final int SHOCK_COOLDOWN = 4;
	private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

	private static UUID pendingEntityId;
	private static double[] pendingOffset;

	private StatusParticleClient() {
	}

	public static UUID consumePendingEntityId() {
		UUID id = pendingEntityId;
		pendingEntityId = null;
		return id;
	}

	public static double[] consumePendingOffset() {
		double[] off = pendingOffset;
		pendingOffset = null;
		return off;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			COOLDOWNS.clear();
			return;
		}

		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof PokemonEntity pokemonEntity)) {
				continue;
			}
			if (mc.player.distanceTo(pokemonEntity) > 48.0) {
				continue;
			}

			PersistentStatusContainer container = pokemonEntity.getPokemon().getStatus();
			if (container == null) {
				COOLDOWNS.remove(pokemonEntity.getUUID());
				continue;
			}
			PersistentStatus status = container.getStatus();
			boolean poison = status == Statuses.POISON || status == Statuses.POISON_BADLY;
			boolean paralysis = status == Statuses.PARALYSIS;
			if (!poison && !paralysis) {
				COOLDOWNS.remove(pokemonEntity.getUUID());
				continue;
			}

			UUID id = pokemonEntity.getUUID();
			int cd = COOLDOWNS.getOrDefault(id, 0);
			if (cd > 0) {
				COOLDOWNS.put(id, cd - 1);
				continue;
			}

			SimpleParticleType type = poison ? CobbleDomesticsModParticleTypes.ENVENE.get() : CobbleDomesticsModParticleTypes.SHOCK.get();
			spawnAnchored(level, pokemonEntity, type);
			COOLDOWNS.put(id, poison ? ENVENE_COOLDOWN : SHOCK_COOLDOWN);
		}
	}

	private static void spawnAnchored(ClientLevel level, PokemonEntity pokemon, SimpleParticleType type) {
		double ox = (level.random.nextDouble() - 0.5) * pokemon.getBbWidth() * 0.85;
		double oy = pokemon.getBbHeight() * (0.25 + level.random.nextDouble() * 0.55);
		double oz = (level.random.nextDouble() - 0.5) * pokemon.getBbWidth() * 0.85;
		pendingEntityId = pokemon.getUUID();
		pendingOffset = new double[]{ox, oy, oz};
		double x = pokemon.getX() + ox;
		double y = pokemon.getY() + oy;
		double z = pokemon.getZ() + oz;
		level.addParticle(type, x, y, z, 0.0, 0.0, 0.0);
	}
}
