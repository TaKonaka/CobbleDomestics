package cobbledomestics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class NotaParticle extends TextureSheetParticle {
	protected NotaParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		this.setSize(0.15F, 0.15F);
		this.quadSize = 0.35F + this.random.nextFloat() * 0.15F;
		this.xd = (this.random.nextDouble() * 2.0 - 1.0) * 0.02;
		this.yd = 0.05 + this.random.nextDouble() * 0.03;
		this.zd = (this.random.nextDouble() * 2.0 - 1.0) * 0.02;
		this.lifetime = 30 + this.random.nextInt(15);
		this.hasPhysics = false;
		this.gravity = 0.0F;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			this.remove();
			return;
		}
		this.move(this.xd, this.yd, this.zd);
		this.xd *= 0.9;
		this.yd *= 0.95;
		this.zd *= 0.9;
		this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			NotaParticle particle = new NotaParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
			particle.pickSprite(this.sprites);
			return particle;
		}
	}
}
