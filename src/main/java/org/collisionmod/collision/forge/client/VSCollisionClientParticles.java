package org.collisionmod.collision.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.collisionmod.collision.forge.VSCollisionConfig;
import org.collisionmod.collision.forge.VSCollisionNetwork;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class VSCollisionClientParticles {

    private static final double BURST_SPAWN_OFFSET = 0.72D;
    private static final double BURST_SPAWN_JITTER = 0.28D;
    private static final double BURST_SPREAD_XZ = 0.70D;
    private static final double BURST_SPREAD_Y = 0.60D;
    private static final double BURST_DIR_XZ = 0.85D;
    private static final double BURST_DIR_Y = 0.75D;
    private static final double BURST_UPWARD = 0.35D;

    private VSCollisionClientParticles() {}

    public static void handle(VSCollisionNetwork.BlockDebrisPacket packet) {
        if (VSCollisionConfig.CLIENT.disableCollisionParticles.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        BlockState state = Block.stateById(packet.stateId());
        if (state.isAir()) {
            return;
        }

        Vector3d normal = normalizeOrUp(packet.normalX(), packet.normalY(), packet.normalZ());
        spawnBurst(level, state, normal, packet);
    }

    private static void spawnBurst(ClientLevel level, BlockState state, Vector3d normal,
                                   VSCollisionNetwork.BlockDebrisPacket packet) {
        double countBase = VSCollisionConfig.COMMON.blockBurstCountBase.get();
        double countPerSpeed = VSCollisionConfig.COMMON.blockBurstCountPerSpeed.get();
        int maxParticles = Math.max(1, (int) Math.floor(VSCollisionConfig.COMMON.maxBlockParticlesPerCollision.get()));
        int blockCount = (int) Math.floor(Math.max(0.0D, Math.min(maxParticles, countBase + packet.impactSpeed() * countPerSpeed)));
        double speedBase = VSCollisionConfig.COMMON.blockBurstSpeedBase.get();
        double speedPerSpeed = VSCollisionConfig.COMMON.blockBurstSpeedPerSpeed.get();
        double speed = Math.max(0.0D, speedBase + packet.impactSpeed() * speedPerSpeed);
        for (int i = 0; i < blockCount; i++) {
            double spreadX = (level.random.nextDouble() - 0.5D) * BURST_SPREAD_XZ;
            double spreadY = (level.random.nextDouble() - 0.5D) * BURST_SPREAD_Y;
            double spreadZ = (level.random.nextDouble() - 0.5D) * BURST_SPREAD_XZ;

            double vx = (normal.x * BURST_DIR_XZ + spreadX) * speed;
            double vy = (normal.y * BURST_DIR_Y + spreadY + BURST_UPWARD) * speed;
            double vz = (normal.z * BURST_DIR_XZ + spreadZ) * speed;

            double sx = packet.x() + normal.x * BURST_SPAWN_OFFSET + (level.random.nextDouble() - 0.5D) * BURST_SPAWN_JITTER;
            double sy = packet.y() + normal.y * BURST_SPAWN_OFFSET + (level.random.nextDouble() - 0.5D) * BURST_SPAWN_JITTER;
            double sz = packet.z() + normal.z * BURST_SPAWN_OFFSET + (level.random.nextDouble() - 0.5D) * BURST_SPAWN_JITTER;

            addDebrisParticle(level, state, sx, sy, sz, vx, vy, vz);
        }
    }

    private static void addDebrisParticle(ClientLevel level, BlockState state, double x, double y, double z,
                                          double vx, double vy, double vz) {
        ImpactTerrainParticle particle = new ImpactTerrainParticle(level, x, y, z, state);
        particle.setParticleSpeed(vx, vy, vz);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static Vector3d normalizeOrUp(double x, double y, double z) {
        Vector3d normal = new Vector3d(x, y, z);
        if (normal.lengthSquared() < 1.0E-8D) {
            normal.set(0.0D, 1.0D, 0.0D);
        } else {
            normal.normalize();
        }
        return normal;
    }

    private static final class ImpactTerrainParticle extends TerrainParticle {
        private ImpactTerrainParticle(ClientLevel level, double x, double y, double z, BlockState state) {
            super(level, x, y, z, 0.0D, 0.0D, 0.0D, state);
            this.hasPhysics = true;
            this.gravity = 0.68F;
            this.friction = 0.96F;
            this.speedUpWhenYMotionIsBlocked = true;
            this.setLifetime(42 + this.random.nextInt(30));
            this.scale(1.05F + this.random.nextFloat() * 0.35F);
        }
    }
}
