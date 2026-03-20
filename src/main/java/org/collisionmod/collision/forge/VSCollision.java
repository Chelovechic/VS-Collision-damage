package org.collisionmod.collision.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
import net.minecraftforge.registries.RegistryObject;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import org.valkyrienskies.core.api.physics.ContactPoint;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Comparator;


@Mod("vs_collision_damage")
public class VSCollision {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "vs_collision_damage");
    // public static final RegistryObject<Item> TESTER = ITEMS.register("tester", () -> new TesterItem(new Item.Properties()));

    public VSCollision() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::init);
        modBus.addListener(this::buildCreativeTabs);
        ITEMS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VSCollisionConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void init(FMLCommonSetupEvent event) {

        VSCollisionEvents.register();
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        // if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
        //     event.accept(TESTER);
        // }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("vsCollision")
                .requires(source -> source.hasPermission(2))
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
            double speedCollider = VSCollisionConfig.COMMON.speedCollider.get();
            double minSpeedExplosion = VSCollisionConfig.COMMON.minSpeedExplosion.get();
            float explosionPower = VSCollisionConfig.COMMON.explosionPower.get().floatValue();
            boolean explosionEnabled = VSCollisionConfig.COMMON.explosionEnabled.get();

            if (impactSpeed < minSpeed) continue;

            int radius = impactSpeed >= minSpeedRadius ? sizeRadius : 0;


            double budgetPerSpeed = VSCollisionConfig.COMMON.budgetPerSpeed.get();
            double costBase = VSCollisionConfig.COMMON.costBase.get();
            double costToughnessMult = VSCollisionConfig.COMMON.costExplosionResMult.get();

            final double initialBudget = impactSpeed * budgetPerSpeed;
            final double[] budget = new double[] { initialBudget };

            int destroyedTotal = 0;
            double explosionPosX = 0;
            double explosionPosY = 0;
            double explosionPosZ = 0;
            boolean explosionPosSet = false;

            if (impactSpeed >= speedCollider) {
                var contacts = collision.getContactPoints().stream()
                        .sorted(Comparator.comparingDouble((ContactPoint cp) -> {
                            Vector3dc v = cp.getVelocity();
                            return v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
                        }).reversed())
                        .toList();

                for (ContactPoint cp : contacts) {
                    if (budget[0] <= 0.0) break;
                    var pos = cp.getPosition();

                    int dA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA.getWorldToShip(), radius, budget, costBase, costToughnessMult);
                    int dB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB.getWorldToShip(), radius, budget, costBase, costToughnessMult);
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

                int destroyedA = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipA.getWorldToShip(), radius, budget, costBase, costToughnessMult);
                int destroyedB = hitBlocksOnShip(level, pos.x(), pos.y(), pos.z(), shipB.getWorldToShip(), radius, budget, costBase, costToughnessMult);
                destroyedTotal = destroyedA + destroyedB;

                if (destroyedTotal > 0) {
                    explosionPosX = pos.x();
                    explosionPosY = pos.y();
                    explosionPosZ = pos.z();
                    explosionPosSet = true;
                }
            }

            if (destroyedTotal > 0) {
                if (explosionEnabled && impactSpeed >= minSpeedExplosion && explosionPosSet) {
                    level.explode(null, explosionPosX, explosionPosY, explosionPosZ, explosionPower, Level.ExplosionInteraction.TNT);
                }
//
                final double finalSpeedA = velA.length();
                final double finalSpeedB = velB.length();
                final double finalCenterRel = centerRelSpeed;
                final double finalContactRel = contactRelSpeed;
                final double finalImpact = impactSpeed;
                final int contactCount = collision.getContactPoints().size();
                final int finalDestroyed = destroyedTotal;

                /*level.players().forEach(p ->
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
               */// );
            }
        }
    }

    private static int hitBlocksOnShip(
            ServerLevel level,
            double worldX,
            double worldY,
            double worldZ,
            Matrix4dc shipWorldToShip,
            int radius,
            double[] budget,
            double costBase,
            double costToughnessMult
    ) {
        Vector3d tmp = new Vector3d();
        shipWorldToShip.transformPosition(worldX, worldY, worldZ, tmp);
        int lx = (int) Math.floor(tmp.x);
        int ly = (int) Math.floor(tmp.y);
        int lz = (int) Math.floor(tmp.z);


        BlockPos centerPos = new BlockPos(lx, ly, lz);
        if (VSGameUtilsKt.isBlockInShipyard(level, centerPos.getX(), centerPos.getY(), centerPos.getZ())) {
                    BlockState state = level.getBlockState(centerPos);
                    var props = CbcToughnessHelper.getBlockProps(level, state, centerPos);


                    if (state.getDestroySpeed(level, centerPos) < 0f && !state.isAir()) {
                return 0;
            }

                    if (!state.isAir() && !state.getCollisionShape(level, centerPos).isEmpty()) {
                        double cost = costBase + (props.toughness() * costToughnessMult);
                if (budget[0] < cost) {

                    return 0;
                }
                budget[0] -= cost;
                level.destroyBlock(centerPos, true);
                if (radius <= 0) {
                    return 1;
                }
            } else if (radius <= 0) {

                return 0;
            }
        } else if (radius <= 0) {
            return 0;
        }

        int destroyed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (budget[0] <= 0.0) return destroyed;

                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos localPos = new BlockPos(lx + dx, ly + dy, lz + dz);
                    if (!VSGameUtilsKt.isBlockInShipyard(level, localPos.getX(), localPos.getY(), localPos.getZ())) continue;

                    BlockState state = level.getBlockState(localPos);
                    if (state.isAir() || state.getDestroySpeed(level, localPos) < 0f) continue;
                    if (state.getCollisionShape(level, localPos).isEmpty()) continue;

                    var props = CbcToughnessHelper.getBlockProps(level, state, localPos);
                    double cost = costBase + (props.toughness() * costToughnessMult);
                    if (budget[0] < cost) continue;
                    budget[0] -= cost;
                    level.destroyBlock(localPos, true);
                    destroyed++;
                }
            }
        }
        return destroyed;
    }
}