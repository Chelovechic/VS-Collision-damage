package org.collisionmod.collision.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.collisionmod.collision.forge.VSCollisionParticles;

@Mod.EventBusSubscriber(modid = "vs_collision_damage", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VSCollisionParticleProviders {

    private VSCollisionParticleProviders() {}

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(VSCollisionParticles.UCK_1.get(), UckParticle.Provider::new);
        event.registerSpriteSet(VSCollisionParticles.UCK_2.get(), UckParticle.Provider::new);
        event.registerSpriteSet(VSCollisionParticles.UCK_3.get(), UckParticle.Provider::new);
        event.registerSpriteSet(VSCollisionParticles.UCK_4.get(), UckParticle.Provider::new);
    }
}
