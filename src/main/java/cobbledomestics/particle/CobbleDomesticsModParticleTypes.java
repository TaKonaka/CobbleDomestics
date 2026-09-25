package cobbledomestics.particle;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, CobbleDomesticsMod.MODID);

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOAP_BUBBLE = REGISTRY.register("soap_bubble", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENVENE = REGISTRY.register("envene", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK = REGISTRY.register("shock", () -> new SimpleParticleType(false));

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACERO = register("acero");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AGUA = register("agua");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON = register("dragon");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRICO = register("electrico");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FANTASMA = register("fantasma");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FUEGO = register("fuego");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HADA = register("hada");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HIELO = register("hielo");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INSECTO = register("insecto");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUCHA = register("lucha");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NORMAL = register("normal");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLANTA = register("planta");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PSIQUICO = register("psiquico");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCA = register("roca");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SINIESTRO = register("siniestro");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TIERRA = register("tierra");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VENENO = register("veneno");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOLADOR = register("volador");

	private CobbleDomesticsModParticleTypes() {
	}

	private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String id) {
		return REGISTRY.register(id, () -> new SimpleParticleType(false));
	}

	public static SimpleParticleType forPokemon(Pokemon pokemon) {
		ElementalType primary = null;
		for (ElementalType type : pokemon.getTypes()) {
			primary = type;
			break;
		}
		return forElementalType(primary);
	}

	public static SimpleParticleType forElementalType(ElementalType type) {
		if (type == null) {
			return NORMAL.get();
		}
		if (type == ElementalTypes.STEEL) {
			return ACERO.get();
		}
		if (type == ElementalTypes.WATER) {
			return AGUA.get();
		}
		if (type == ElementalTypes.DRAGON) {
			return DRAGON.get();
		}
		if (type == ElementalTypes.ELECTRIC) {
			return ELECTRICO.get();
		}
		if (type == ElementalTypes.GHOST) {
			return FANTASMA.get();
		}
		if (type == ElementalTypes.FIRE) {
			return FUEGO.get();
		}
		if (type == ElementalTypes.FAIRY) {
			return HADA.get();
		}
		if (type == ElementalTypes.ICE) {
			return HIELO.get();
		}
		if (type == ElementalTypes.BUG) {
			return INSECTO.get();
		}
		if (type == ElementalTypes.FIGHTING) {
			return LUCHA.get();
		}
		if (type == ElementalTypes.NORMAL) {
			return NORMAL.get();
		}
		if (type == ElementalTypes.GRASS) {
			return PLANTA.get();
		}
		if (type == ElementalTypes.PSYCHIC) {
			return PSIQUICO.get();
		}
		if (type == ElementalTypes.ROCK) {
			return ROCA.get();
		}
		if (type == ElementalTypes.DARK) {
			return SINIESTRO.get();
		}
		if (type == ElementalTypes.GROUND) {
			return TIERRA.get();
		}
		if (type == ElementalTypes.POISON) {
			return VENENO.get();
		}
		if (type == ElementalTypes.FLYING) {
			return VOLADOR.get();
		}
		return NORMAL.get();
	}
}
