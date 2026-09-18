package cobbledomestics.particle;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, CobbleDomesticsMod.MODID);

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOAP_BUBBLE = REGISTRY.register("soap_bubble", () -> new SimpleParticleType(false));

	private CobbleDomesticsModParticleTypes() {
	}
}
