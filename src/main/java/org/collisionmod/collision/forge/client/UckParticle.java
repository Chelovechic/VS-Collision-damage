package org.collisionmod.collision.forge.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.collisionmod.collision.forge.VSCollisionConfig;

public final class UckParticle extends TextureSheetParticle {
    private UckParticle(ClientLevel level, double x, double y, double z,
                        double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        this.hasPhysics = false;
        this.friction = 0.985F;
        this.gravity = 0.68F;
        this.quadSize *= 0.9F + this.random.nextFloat() * 0.35F;
        this.lifetime = 24 + this.random.nextInt(20);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setAlpha(1.0F - (this.age / (float) this.lifetime));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            if (VSCollisionConfig.CLIENT.disableCollisionParticles.get()) {
                return null;
            }
            return new UckParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
