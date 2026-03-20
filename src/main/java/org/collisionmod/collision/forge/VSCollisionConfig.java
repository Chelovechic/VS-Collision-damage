package org.collisionmod.collision.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public class VSCollisionConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static class Common {

        public final ForgeConfigSpec.ConfigValue<Double> minSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> minSpeedRadius;
        public final ForgeConfigSpec.ConfigValue<Integer> sizeRadius;
        public final ForgeConfigSpec.ConfigValue<Double> speedCollider;

        public final ForgeConfigSpec.ConfigValue<Double> minSpeedExplosion;
        public final ForgeConfigSpec.ConfigValue<Double> explosionPower;
        public final ForgeConfigSpec.BooleanValue explosionEnabled;

        public final ForgeConfigSpec.ConfigValue<Double> budgetPerSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> costBase;
        public final ForgeConfigSpec.ConfigValue<Double> costExplosionResMult;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("general_ship-ship");

            minSpeed = builder
                    .comment("Мин. скорость")
                    .define("minSpeed", 7.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            minSpeedRadius = builder
                    .comment("Min радиус")
                    .define("minSpeedRadius", 40.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            sizeRadius = builder
                    .comment("радиус, 0 = 1 бл, 1 = 3x3x3")
                    .define("sizeRadius", 1, o -> o instanceof Integer i && i >= 0 && i <= 1000);

            builder.comment("\n");

            speedCollider = builder
                    .comment("использовать все точки контакта")
                    .define("speedCollider", 15.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            minSpeedExplosion = builder
                    .comment("Мин. скорость для взрыва")
                    .define("minSpeedExplosion", 50.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            explosionPower = builder
                    .comment("Мощность взрыва")
                    .define("explosionPower", 2.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            explosionEnabled = builder
                    .comment("Взрыв вкл/выкл")
                    .define("explosionEnabled", true);

            builder.comment("\n");

            budgetPerSpeed = builder
                    .comment("Cost на 1м/с")
                    .define("budgetPerSpeed", 1.1, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.comment("\n");

            costBase = builder
                    .comment("Min cost")
                    .define("costBase", 1.0, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.comment("\n");

            costExplosionResMult = builder
                    .comment("Множитель cost от toughness")
                    .define("costToughnessMult", 2.0, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.pop();
        }
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }
}