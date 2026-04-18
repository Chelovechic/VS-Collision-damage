package org.collisionmod.collision.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.joml.Vector3dc;

import java.lang.reflect.Method;

public final class VSCollisionNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final double PARTICLE_SYNC_RADIUS = 96.0D;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("vs_collision_damage", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered = false;
    private static volatile Method blockDebrisClientHandler;

    private VSCollisionNetwork() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(BlockDebrisPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BlockDebrisPacket::encode)
                .decoder(BlockDebrisPacket::decode)
                .consumerMainThread((message, contextSupplier) -> dispatchBlockDebris(message))
                .noResponse()
                .add();
    }

    public static void sendBlockDebrisBurst(ServerLevel level, double x, double y, double z,
                                            BlockState state, Vector3dc normal, double impactSpeed) {
        if (state.isAir()) {
            return;
        }

        CHANNEL.send(
                PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, PARTICLE_SYNC_RADIUS, level.dimension())),
                new BlockDebrisPacket(x, y, z, Block.getId(state),
                        normal.x(), normal.y(), normal.z(), impactSpeed)
        );
    }

    private static void dispatchBlockDebris(BlockDebrisPacket message) {
        try {
            Method handler = blockDebrisClientHandler;
            if (handler == null) {
                Class<?> handlerClass = Class.forName("org.collisionmod.collision.forge.client.VSCollisionClientParticles");
                handler = handlerClass.getMethod("handle", BlockDebrisPacket.class);
                blockDebrisClientHandler = handler;
            }
            handler.invoke(null, message);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("error", e);
        }
    }

    public record BlockDebrisPacket(
            double x,
            double y,
            double z,
            int stateId,
            double normalX,
            double normalY,
            double normalZ,
            double impactSpeed
    ) {
        private static void encode(BlockDebrisPacket message, FriendlyByteBuf buf) {
            buf.writeDouble(message.x);
            buf.writeDouble(message.y);
            buf.writeDouble(message.z);
            buf.writeVarInt(message.stateId);
            buf.writeDouble(message.normalX);
            buf.writeDouble(message.normalY);
            buf.writeDouble(message.normalZ);
            buf.writeDouble(message.impactSpeed);
        }

        private static BlockDebrisPacket decode(FriendlyByteBuf buf) {
            return new BlockDebrisPacket(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            );
        }
    }
}
