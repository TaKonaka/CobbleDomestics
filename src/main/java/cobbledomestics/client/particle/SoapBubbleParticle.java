package cobbledomestics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SoapBubbleParticle extends TextureSheetParticle {
	protected SoapBubbleParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		this.setSize(0.02F, 0.02F);
		this.quadSize *= this.random.nextFloat() * 0.6F + 0.4F;
		this.xd = xSpeed * 0.2 + (this.random.nextDouble() * 2.0 - 1.0) * 0.02;
		this.yd = ySpeed * 0.2 + 0.04 + this.random.nextDouble() * 0.02;
		this.zd = zSpeed * 0.2 + (this.random.nextDouble() * 2.0 - 1.0) * 0.02;
		this.lifetime = 25 + this.random.nextInt(20);
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
		this.yd += 0.002;
		this.move(this.xd, this.yd, this.zd);
		this.xd *= 0.85;
		this.yd *= 0.85;
		this.zd *= 0.85;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			SoapBubbleParticle particle = new SoapBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
			particle.pickSprite(this.sprites);
			return particle;
		}
	}
}
