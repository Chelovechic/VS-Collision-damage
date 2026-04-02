package org.collisionmod.collision.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegistryObject;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBic;

import org.valkyrienskies.core.api.physics.ContactPoint;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Comparator;


@Mod("vs_collision_damage")
public class VSCollision {

    //вкл-выкл логи
    private static final boolean additional_debug = false;

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "vs_collision_damage");
    // public static final RegistryObject<Item> TESTER = ITEMS.register("tester", () -> new TesterItem(new Item.Properties()));

    public VSCollision() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::init);
        //modBus.addListener(this::buildCreativeTabs);
        ITEMS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VSCollisionConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void init(FMLCommonSetupEvent event) {

        VSCollisionEvents.register();
    }
    /*
        private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
             if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                 event.accept(TESTER);
             }
        }
    */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("vsCollision")
                        .requires(source -> source.hasPermission(2)) // только операторы
                        .then(Commands.literal("mode")
                                .then(Commands.literal("PhysX")
                                        .executes(ctx -> {
                                            VSCollisionEvents.MODE = VSCollisionEvents.BackendMode.PHYSX;
                                            CommandSourceStack src = ctx.getSource();
                                            src.sendSuccess(() -> Component.literal("PhysX mode enabled (recommended) - only PhysX backend (collisionStartEvent)"), true);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("Krunch")
                                        .executes(ctx -> {
                                            VSCollisionEvents.MODE = VSCollisionEvents.BackendMode.KRUNCH;
                                            CommandSourceStack src = ctx.getSource();
                                            src.sendSuccess(() -> Component.literal("Krunch mode enabled (not recommended) - Krunch and PhysX backend (CollisionPersistEvent)"), true);
                                            return 1;
                                        })
                                )
                        )
        );
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        while (true) {
            var collision = VSCollisionEvents.QUEUE.poll();
            if (collision == null) break;

            var dimId = collision.getDimensionId();
            ServerLevel level = VSGameUtilsKt.getLevelFromDimensionId(server, dimId);
            if (level == null) continue;

            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            LoadedServerShip shipA = shipWorld.getLoadedShips().getById(collision.getShipIdA());
            LoadedServerShip shipB = shipWorld.getLoadedShips().getById(collision.getShipIdB());
            if (shipA == null || shipB == null) continue;

            double massA = shipA.getInertiaData().getMass();
            double massB = shipB.getInertiaData().getMass();

            Vector3dc velA = shipA.getVelocity();
            Vector3dc velB = shipB.getVelocity();
            Vector3d centerRelVec = new Vector3d(velA).sub(velB);
            double centerRelSpeed = centerRelVec.length();

            double contactRelSpeed = collision.getContactPoints().stream()
                    .mapToDouble(cp -> cp.getVelocity().length())
                    .max()
                    .orElse(0.0);

            double impactSpeed = Math.max(contactRelSpeed, centerRelSpeed);

            double minSpeed = VSCollisionConfig.COMMON.minSpeed.get();
            double minSpeedRadius = VSCollisionConfig.COMMON.minSpeedRadius.get();
            int sizeRadius = VSCollisionConfig.COMMON.sizeRadius.get();
            boolean radiusEnabled = VSCollisionConfig.COMMON.radiusEnabled.get();
            double speedCollider = VSCollisionConfig.COMMON.speedCollider.get();
            double minSpeedExplosion = VSCollisionConfig.COMMON.minSpeedExplosion.get();
            float explosionPower = VSCollisionConfig.COMMON.explosionPower.get().floatValue();
            boolean explosionEnabled = VSCollisionConfig.COMMON.explosionEnabled.get();

            if (impactSpeed < minSpeed) continue;

            int radius = (radiusEnabled && impactSpeed >= minSpeedRadius) ? sizeRadius : 0;


            double budgetPerSpeed = VSCollisionConfig.COMMON.budgetPerSpeed.get();
            boolean budgetUseMass = VSCollisionConfig.COMMON.budgetUseMass.get();
            //boolean filterWeakContacts = VSCollisionConfig.COMMON.filterWeakContacts.get();
            //double weakContactSpeedFrac = VSCollisionConfig.COMMON.weakContactSpeedFrac.get();
            double massScale = VSCollisionConfig.COMMON.costMassScale.get();
            double costBase = VSCollisionConfig.COMMON.costBase.get();
            double costToughnessMult = VSCollisionConfig.COMMON.costExplosionResMult.get();
/*
            final double initialBudgetA = impactSpeed * budgetPerSpeed + (budgetUseMass ? (massA * massScale) : 0.0);
            final double initialBudgetB = impactSpeed * budgetPerSpeed + (budgetUseMass ? (massB * massScale) : 0.0);
*/

            final double initialBudgetA = impactSpeed * budgetPerSpeed + (budgetUseMass ? (massB * massScale) : 0.0);

            final double initialBudgetB = impactSpeed * budgetPerSpeed + (budgetUseMass ? (massA * massScale) : 0.0);


            int destroyedTotal = 0;
            double explosionPosX = 0;
            double explosionPosY = 0;
            double explosionPosZ = 0;
            boolean explosionPosSet = false;

            if (impactSpeed >= speedCollider) {
                var sortedContacts = collision.getContactPoints().stream()
                        .sorted(Comparator.comparingDouble((ContactPoint cp) -> {
                            Vector3dc v = cp.getVelocity();
                            return v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
                        }).reversed())
                        .toList();
                if (sortedContacts.isEmpty()) continue;

                var contacts = sortedContacts;
             /*   if (filterWeakContacts) {
                    double maxCpSpeed = sortedContacts.get(0).getVelocity().length();
                    double minAllowed = maxCpSpeed * weakContactSpeedFrac;
                    var filtered = sortedContacts.stream()
                            .filter(cp -> cp.getVelocity().length() >= minAllowed)
                            .toList();
                    if (!filtered.isEmpty()) {
                        contacts = filtered;
                    }
                }
*/

                int n = contacts.size();
                if (n <= 0) continue;

                double minEqualFraction = 0.3;
                double epsilonWeight = 1.0e-4;
                double[] weights = new double[n];
                double sumW = 0.0;
                for (int i = 0; i < n; i++) {
                    ContactPoint cp = contacts.get(i);
                    double w = cp.getVelocity().lengthSquared();
                    if (w < epsilonWeight) w = epsilonWeight;
                    weights[i] = w;
                    sumW += w;
                }

                for (int i = 0; i < n; i++) {
                    ContactPoint cp = contacts.get(i);
                    var pos = cp.getPosition();
                    var normal = cp.getNormal();

                    double equalBudgetA = (initialBudgetA * minEqualFraction) / n;
                    double weightedBudgetA = initialBudgetA * (1.0 - minEqualFraction) * (weights[i] / sumW);
                    double contactBudgetAValue = equalBudgetA + weightedBudgetA;

                    double equalBudgetB = (initialBudgetB * minEqualFraction) / n;
                    double weightedBudgetB = initialBudgetB * (1.0 - minEqualFraction) * (weights[i] / sumW);
                    double contactBudgetBValue = equalBudgetB + weightedBudgetB;

                    double[] contactBudgetA = new double[]{contactBudgetAValue};
                    double[] contactBudgetB = new double[]{contactBudgetBValue};

                    int dA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA, normal, massB, impactSpeed, radius, contactBudgetA, costBase, costToughnessMult);
                    int dB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB, new Vector3d(normal).negate(), massA, impactSpeed, radius, contactBudgetB, costBase, costToughnessMult);
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
                var normal = contact.getNormal();


                double[] budgetA = new double[]{initialBudgetA};
                double[] budgetB = new double[]{initialBudgetB};

                int destroyedA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA, normal, massB, impactSpeed, radius, budgetA, costBase, costToughnessMult);
                int destroyedB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB, new Vector3d(normal).negate(), massA, impactSpeed, radius, budgetB, costBase, costToughnessMult);
                destroyedTotal = destroyedA + destroyedB;

                if (destroyedTotal > 0) {
                    explosionPosX = pos.x();
                    explosionPosY = pos.y();
                    explosionPosZ = pos.z();
                    explosionPosSet = true;
                }
            }

            if (destroyedTotal > 0 && explosionEnabled && impactSpeed >= minSpeedExplosion && explosionPosSet) {
                level.explode(null, explosionPosX, explosionPosY, explosionPosZ, explosionPower, Level.ExplosionInteraction.TNT);
            }
            if (additional_debug) {
                final double finalSpeedA = velA.length();
                final double finalSpeedB = velB.length();
                final double finalCenterRel = centerRelSpeed;
                final double finalContactRel = contactRelSpeed;
                final double finalImpact = impactSpeed;
                final int contactCount = collision.getContactPoints().size();
                final int finalDestroyed = destroyedTotal;
//модуль отладки
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
                );
            }
        }
    }

    private static BlockPos chooseDeepDirLocal(Vector3d dirLocal) {
        if (dirLocal.lengthSquared() < 1.0e-6) {
            return null;
        }
        double ax = Math.abs(dirLocal.x());
        double ay = Math.abs(dirLocal.y());
        double az = Math.abs(dirLocal.z());
        if (ax >= ay && ax >= az) {
            return new BlockPos(dirLocal.x() > 0.0 ? 1 : -1, 0, 0);
        }
        if (ay >= ax && ay >= az) {
            return new BlockPos(0, dirLocal.y() > 0.0 ? 1 : -1, 0);
        }
        return new BlockPos(0, 0, dirLocal.z() > 0.0 ? 1 : -1);
    }

    private static int rayLengthToShipBoundary(BlockPos localPos, BlockPos deepDir, AABBic localAABB) {
        int lx = localPos.getX();
        int ly = localPos.getY();
        int lz = localPos.getZ();
        int dx = deepDir.getX();
        int dy = deepDir.getY();
        int dz = deepDir.getZ();
        int minX = localAABB.minX();
        int minY = localAABB.minY();
        int minZ = localAABB.minZ();
        int maxX = localAABB.maxX();
        int maxY = localAABB.maxY();
        int maxZ = localAABB.maxZ();

        int kx = stepsAlongAxis(lx, dx, minX, maxX);
        int ky = stepsAlongAxis(ly, dy, minY, maxY);
        int kz = stepsAlongAxis(lz, dz, minZ, maxZ);
        return Math.min(kx, Math.min(ky, kz));
    }

    private static int stepsAlongAxis(int pos, int dir, int minB, int maxB) {
        if (dir > 0) {
            return Math.max(0, maxB - 1 - pos);
        }
        if (dir < 0) {
            return Math.max(0, pos - minB);
        }
        return Integer.MAX_VALUE;
    }

    private static boolean isInsideShipLocalBounds(BlockPos p, AABBic aabb) {
        int x = p.getX();
        int y = p.getY();
        int z = p.getZ();
        return x >= aabb.minX() && x < aabb.maxX()
                && y >= aabb.minY() && y < aabb.maxY()
                && z >= aabb.minZ() && z < aabb.maxZ();
    }


    private static int computeColumnExtraDepth(double impactSpeedMs, double attackerMassKg, int rayVoxelLength) {
        if (rayVoxelLength <= 1) {
            return 0;
        }
        double speedL = VSCollisionConfig.COMMON.shipSpeedL.get();
        double massPerBlock = VSCollisionConfig.COMMON.shipMass.get();
        int maxDepth = VSCollisionConfig.COMMON.maxDepth.get();

        double resDamage = impactSpeedMs * speedL + attackerMassKg * massPerBlock;
        int extra = (int) Math.floor(resDamage);
        extra = Math.min(extra, Math.max(0, maxDepth - 1));
        extra = Math.min(extra, rayVoxelLength - 1);
        return Math.max(0, extra);
    }


    private static int hitBlocksOnShip(
            ServerLevel level,
            double worldX,
            double worldY,
            double worldZ,
            LoadedServerShip ship,
            Vector3dc worldNormal,
            double attackerMassKg,
            double impactSpeed,
            int radius,
            double[] budget,
            double costBase,
            double costToughnessMult
    ) {
        Matrix4dc shipWorldToShip = ship.getWorldToShip();
        Vector3d posTmp = new Vector3d();
        shipWorldToShip.transformPosition(worldX, worldY, worldZ, posTmp);
        int lx = (int) Math.floor(posTmp.x);
        int ly = (int) Math.floor(posTmp.y);
        int lz = (int) Math.floor(posTmp.z);

        Vector3d dirLocal = new Vector3d();
        shipWorldToShip.transformDirection(new Vector3d(worldNormal), dirLocal);
        BlockPos deepDir = chooseDeepDirLocal(dirLocal);

        BlockPos centerPos = new BlockPos(lx, ly, lz);
        int destroyed = 0;


        double targetMassKg = ship.getInertiaData().getMass();
        double massCostMul = 1.0;

        if (VSGameUtilsKt.isBlockInShipyard(level, centerPos.getX(), centerPos.getY(), centerPos.getZ())) {
            BlockState state = level.getBlockState(centerPos);
            var props = CbcToughnessHelper.getBlockProps(level, state, centerPos);

            if (state.getDestroySpeed(level, centerPos) < 0f && !state.isAir()) {
                return 0;
            }

            if (!state.isAir() && !state.getCollisionShape(level, centerPos).isEmpty()) {
                double cost = (costBase + (props.toughness() * costToughnessMult)) * massCostMul;
                if (budget[0] < cost) {
                    return 0;
                }
                budget[0] -= cost;
                level.destroyBlock(centerPos, true);
                destroyed++;
            }
        } else if (radius <= 0 && deepDir == null) {
            return 0;
        }


        AABBic localAABB = ship.getShipAABB();
        if (VSCollisionConfig.COMMON.rayEnabled.get()
                && impactSpeed >= VSCollisionConfig.COMMON.rayMinSpeed.get()
                && deepDir != null && localAABB != null && budget[0] > 0.0) {
            int rayLen = rayLengthToShipBoundary(centerPos, deepDir, localAABB);
            int extra = computeColumnExtraDepth(impactSpeed, attackerMassKg, rayLen);
            int innerRayDestroyed = 0;
            for (int step = 1; step <= extra; step++) {
                if (budget[0] <= 0.0) {
                    break;
                }
                BlockPos inner = centerPos.offset(deepDir.getX() * step, deepDir.getY() * step, deepDir.getZ() * step);
                if (!isInsideShipLocalBounds(inner, localAABB)) {
                    continue;
                }
                if (!VSGameUtilsKt.isBlockInShipyard(level, inner.getX(), inner.getY(), inner.getZ())) {
                    continue;
                }
                BlockState innerState = level.getBlockState(inner);

                if (innerState.isAir()) {
                    continue;
                }
                if (innerState.getDestroySpeed(level, inner) < 0f) {
                    break;
                }
                if (innerState.getCollisionShape(level, inner).isEmpty()) {
                    continue;
                }
                var innerProps = CbcToughnessHelper.getBlockProps(level, innerState, inner);
                double innerCost = (costBase + (innerProps.toughness() * costToughnessMult)) * massCostMul;
                if (budget[0] < innerCost) {
                    break;
                }
                budget[0] -= innerCost;
                level.destroyBlock(inner, true);
                destroyed++;
                innerRayDestroyed++;
            }
            if (additional_debug && (extra > 0 || innerRayDestroyed > 0)) {
                double speedL = VSCollisionConfig.COMMON.shipSpeedL.get();
                double massK = VSCollisionConfig.COMMON.shipMass.get();
                double resDamage = impactSpeed * speedL + attackerMassKg * massK;
                String chat = String.format(
                        "debug | цель id=%d B=%.2f кг | A=%.2f кг | impact=%.4f | res=%.4f | blocks=%d | desBlocks=%d",
                        ship.getId(),
                        targetMassKg,
                        attackerMassKg,
                        impactSpeed,
                        resDamage,
                        extra,
                        innerRayDestroyed
                );
                level.players().forEach(p -> p.sendSystemMessage(Component.literal(chat)));
            }
        }

        if (radius <= 0) {
            return destroyed;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (budget[0] <= 0.0) {
                        return destroyed;
                    }
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos localPos = new BlockPos(lx + dx, ly + dy, lz + dz);
                    if (!VSGameUtilsKt.isBlockInShipyard(level, localPos.getX(), localPos.getY(), localPos.getZ())) {
                        continue;
                    }

                    BlockState state = level.getBlockState(localPos);
                    if (state.isAir() || state.getDestroySpeed(level, localPos) < 0f) {
                        continue;
                    }
                    if (state.getCollisionShape(level, localPos).isEmpty()) {
                        continue;
                    }

                    var props = CbcToughnessHelper.getBlockProps(level, state, localPos);
                    double cost = (costBase + (props.toughness() * costToughnessMult)) * massCostMul;
                    if (budget[0] < cost) {
                        continue;
                    }
                    budget[0] -= cost;
                    level.destroyBlock(localPos, true);
                    destroyed++;
                }
            }
        }
        return destroyed;
    }
}