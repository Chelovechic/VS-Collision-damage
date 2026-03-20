package org.collisionmod.collision.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CbcToughnessHelper {

    private static final String HANDLER_CLASS = "rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler";

    public static record BlockProps(double hardness, double toughness, boolean fromCbc) {}

    public static BlockProps getBlockProps(Level level, BlockState state, BlockPos pos) {
        try {
            Class<?> handlerClass = Class.forName(HANDLER_CLASS);
            var getProperties = handlerClass.getMethod("getProperties", BlockState.class);
            Object provider = getProperties.invoke(null, state);
            if (provider != null) {
                var toughnessMethod = provider.getClass().getMethod("toughness",
                        Level.class, BlockState.class, BlockPos.class, boolean.class);
                var hardnessMethod = provider.getClass().getMethod("hardness",
                        Level.class, BlockState.class, BlockPos.class, boolean.class);

                double toughness = (Double) toughnessMethod.invoke(provider, level, state, pos, true);
                double hardness = (Double) hardnessMethod.invoke(provider, level, state, pos, true);

                return new BlockProps(hardness, toughness, true);
            }
        } catch (Throwable ignored) {
            // -CBC
        }


        double h = 1.0;
        double t = state.getExplosionResistance(level, pos, null);
        return new BlockProps(h, t, false);
    }
}

