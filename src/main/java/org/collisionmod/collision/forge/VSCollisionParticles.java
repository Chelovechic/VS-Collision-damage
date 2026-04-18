package org.collisionmod.collision.forge;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VSCollisionParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "vs_collision_damage");

    public static final RegistryObject<SimpleParticleType> UCK_1 = PARTICLES.register("uck_1", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> UCK_2 = PARTICLES.register("uck_2", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> UCK_3 = PARTICLES.register("uck_3", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> UCK_4 = PARTICLES.register("uck_4", () -> new SimpleParticleType(true));

    private VSCollisionParticles() {}
}
