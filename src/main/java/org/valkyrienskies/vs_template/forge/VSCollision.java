package org.valkyrienskies.vs_template.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import org.valkyrienskies.core.api.events.CollisionEvent;
import org.valkyrienskies.core.api.physics.ContactPoint;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;


//maralys иди нахуй


@Mod("vs_collision_damage")
public class VSCollision {

    private static final double MIN_SPEED = 7.0;

    private static final double MIN_SPEED_RADIUS = 40;
    private static final int SIZE_RADIUS = 1;

    private static final double SPEED_COLLIDER = 15.0;
    private static final int MAX_BLOCKS = 64000000;

    private static final double MIN_SPEED_EXPOSION = 30.0;
    private static final float EXPLOSION_POWER = 2.0f;
    private static final boolean EXPLOSION = true;

    private static final ConcurrentLinkedQueue<CollisionEvent> collisionQueue = new ConcurrentLinkedQueue<>();

    public VSCollision() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::init);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void init(FMLCommonSetupEvent event) {
        ValkyrienSkiesMod.getApi().getCollisionStartEvent().on(ev -> collisionQueue.add(ev));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        while (true) {
            CollisionEvent collision = collisionQueue.poll();
            if (collision == null) break;

            var dimId = collision.getDimensionId();
            ServerLevel level = VSGameUtilsKt.getLevelFromDimensionId(server, dimId);
            if (level == null) continue;

            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            LoadedServerShip shipA = shipWorld.getLoadedShips().getById(collision.getShipIdA());
            LoadedServerShip shipB = shipWorld.getLoadedShips().getById(collision.getShipIdB());
            if (shipA == null || shipB == null) continue;

            Vector3dc velA = shipA.getVelocity();
            Vector3dc velB = shipB.getVelocity();
            Vector3d centerRelVec = new Vector3d(velA).sub(velB);
            double centerRelSpeed = centerRelVec.length();

            double contactRelSpeed = collision.getContactPoints().stream()
                    .mapToDouble(cp -> cp.getVelocity().length())
                    .max()
                    .orElse(0.0);

            double impactSpeed = Math.max(contactRelSpeed, centerRelSpeed);

            if (impactSpeed < MIN_SPEED) continue;

            int radius = impactSpeed >= MIN_SPEED_RADIUS ? SIZE_RADIUS : 0;

            int destroyedTotal = 0;
            double explosionPosX = 0;
            double explosionPosY = 0;
            double explosionPosZ = 0;
            boolean explosionPosSet = false;

            if (impactSpeed >= SPEED_COLLIDER) {
                var contacts = collision.getContactPoints().stream()
                        .sorted(Comparator.comparingDouble((ContactPoint cp) -> {
                            Vector3dc v = cp.getVelocity();
                            return v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
                        }).reversed())
                        .limit(MAX_BLOCKS)
                        .toList();

                for (ContactPoint cp : contacts) {
                    var pos = cp.getPosition();

                    int dA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA.getWorldToShip(), radius);
                    int dB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB.getWorldToShip(), radius);
                    int d = dA + dB;
                    destroyedTotal += d;

                    if (!explosionPosSet && d > 0) {
                        explosionPosX = pos.x();
                        explosionPosY = pos.y();
                        explosionPosZ = pos.z();
                        explosionPosSet = true;
                    }
                }
            } else {
                ContactPoint contact = collision.getContactPoints().stream()
                        .max(Comparator.comparingDouble(cp -> {
                            Vector3dc v = cp.getVelocity();
                            return v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
                        }))
                        .orElse(null);
                if (contact == null) continue;

                var pos = contact.getPosition();

                int destroyedA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA.getWorldToShip(), radius);
                int destroyedB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB.getWorldToShip(), radius);
                destroyedTotal = destroyedA + destroyedB;

                if (destroyedTotal > 0) {
                    explosionPosX = pos.x();
                    explosionPosY = pos.y();
                    explosionPosZ = pos.z();
                    explosionPosSet = true;
                }
            }

            if (destroyedTotal > 0) {
                if (EXPLOSION && impactSpeed >= MIN_SPEED_EXPOSION && explosionPosSet) {
                    level.explode(null, explosionPosX, explosionPosY, explosionPosZ, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
                }
                /*
                final double finalSpeedA = velA.length();
                final double finalSpeedB = velB.length();
                final double finalCenterRel = centerRelSpeed;
                final double finalContactRel = contactRelSpeed;
                final double finalImpact = impactSpeed;
                final int contactCount = collision.getContactPoints().size();
                final int finalDestroyed = destroyedTotal;

                level.players().forEach(p ->
                        p.sendSystemMessage(
                                Component.literal(
                                        "DEBUG Collision | " +
                                                "A=" + String.format("%.2f", finalSpeedA) + " | " +
                                                "B=" + String.format("%.2f", finalSpeedB) + " | " +
                                                "centerRel=" + String.format("%.2f", finalCenterRel) + " | " +
                                                "contactRel=" + String.format("%.2f", finalContactRel) + " | " +
                                                "IMPACT=" + String.format("%.2f", finalImpact) + " | " +
                                                "contacts=" + contactCount + " | " +
                                                "destroyed=" + finalDestroyed
                                )
                        )
                );*/
            }
        }
    }

    private static int hitBlocksOnShip(
            ServerLevel level,
            double worldX,
            double worldY,
            double worldZ,
            Matrix4dc shipWorldToShip,
            int radius
    ) {
        Vector3d tmp = new Vector3d();
        shipWorldToShip.transformPosition(worldX, worldY, worldZ, tmp);
        int lx = (int) Math.floor(tmp.x);
        int ly = (int) Math.floor(tmp.y);
        int lz = (int) Math.floor(tmp.z);

        if (radius <= 0) {
            BlockPos localPos = new BlockPos(lx, ly, lz);
            if (VSGameUtilsKt.isBlockInShipyard(level, localPos.getX(), localPos.getY(), localPos.getZ())) {
                BlockState state = level.getBlockState(localPos);
                if (!state.isAir() && state.getDestroySpeed(level, localPos) >= 0f && !state.getCollisionShape(level, localPos).isEmpty()) {
                    level.destroyBlock(localPos, true);
                    return 1;
                }
            }
            return 0;
        }

        int destroyed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos localPos = new BlockPos(lx + dx, ly + dy, lz + dz);
                    if (!VSGameUtilsKt.isBlockInShipyard(level, localPos.getX(), localPos.getY(), localPos.getZ())) continue;

                    BlockState state = level.getBlockState(localPos);
                    if (state.isAir() || state.getDestroySpeed(level, localPos) < 0f) continue;
                    if (state.getCollisionShape(level, localPos).isEmpty()) continue;

                    level.destroyBlock(localPos, true);
                    destroyed++;
                }
            }
        }
        return destroyed;
    }
}