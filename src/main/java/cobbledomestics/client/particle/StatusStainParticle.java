package cobbledomestics.client.particle;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;

/**
 * Billboard stain/spark that stays glued to a Pokémon (no gravity, no fall).
 * xd/yd/zd unused; entity UUID is encoded into spawn coords via a client-side spawner.
 */
public class StatusStainParticle extends TextureSheetParticle {
	private final UUID entityId;
	private final double offsetX;
	private final double offsetY;
	private final double offsetZ;
	private final SpriteSet sprites;
	private final boolean animateSprites;

	protected StatusStainParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
			UUID entityId, double offsetX, double offsetY, double offsetZ, int lifetime, float size, boolean animateSprites) {
		super(level, x, y, z, 0.0, 0.0, 0.0);
		this.sprites = sprites;
		this.entityId = entityId;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.animateSprites = animateSprites;
		this.setSize(size, size);
		this.quadSize = size;
		this.lifetime = lifetime;
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.xd = 0.0;
		this.yd = 0.0;
		this.zd = 0.0;
		this.setSpriteFromAge(sprites);
		snapToEntity();
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
		if (!snapToEntity()) {
			this.remove();
			return;
		}
		if (animateSprites) {
			this.setSpriteFromAge(this.sprites);
		}
		float life = (float) this.age / (float) this.lifetime;
		if (life < 0.15F) {
			this.alpha = life / 0.15F;
		} else if (life > 0.7F) {
			this.alpha = 1.0F - (life - 0.7F) / 0.3F;
		} else {
			this.alpha = 1.0F;
		}
	}

	private boolean snapToEntity() {
		Entity entity = findEntity();
		if (entity == null || entity.isRemoved()) {
			return false;
		}
		this.setPos(entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ);
		return true;
	}

	private Entity findEntity() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return null;
		}
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entityId.equals(entity.getUUID())) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class EnveneProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public EnveneProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			// Spawner packs: xSpeed/ySpeed/zSpeed unused; entity UUID and offsets come from StatusParticleClient
			UUID id = StatusParticleClient.consumePendingEntityId();
			double[] off = StatusParticleClient.consumePendingOffset();
			if (id == null || off == null) {
				return null;
			}
			int life = 40 + level.random.nextInt(20);
			float size = 0.28F + level.random.nextFloat() * 0.12F;
			return new StatusStainParticle(level, x, y, z, sprites, id, off[0], off[1], off[2], life, size, true);
		}
	}

	public static class ShockProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public ShockProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			UUID id = StatusParticleClient.consumePendingEntityId();
			double[] off = StatusParticleClient.consumePendingOffset();
			if (id == null || off == null) {
				return null;
			}
			int life = 6 + level.random.nextInt(8);
			float size = 0.18F + level.random.nextFloat() * 0.1F;
			return new StatusStainParticle(level, x, y, z, sprites, id, off[0], off[1], off[2], life, size, true);
		}
	}
}
