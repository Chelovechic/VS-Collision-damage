package org.collisionmod.collision.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public class VSCollisionConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final org.collisionmod.collision.forge.VSCollisionConfig.Common COMMON;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final VisualClientConfig CLIENT;

    public static class Common {

        public final ForgeConfigSpec.ConfigValue<Double> minSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> minSpeedRadius;
        public final ForgeConfigSpec.ConfigValue<Integer> sizeRadius;
        public final ForgeConfigSpec.ConfigValue<Double> speedCollider;

        public final ForgeConfigSpec.ConfigValue<Double> minSpeedExplosion;
        public final ForgeConfigSpec.ConfigValue<Double> explosionPower;
        public final ForgeConfigSpec.BooleanValue explosionEnabled;

        public final ForgeConfigSpec.ConfigValue<Double> budgetPerSpeed;

        public final ForgeConfigSpec.BooleanValue budgetUseMass;

        //public final ForgeConfigSpec.BooleanValue filterWeakContacts;

        //public final ForgeConfigSpec.ConfigValue<Double> weakContactSpeedFrac;
        public final ForgeConfigSpec.ConfigValue<Double> costBase;
        public final ForgeConfigSpec.ConfigValue<Double> costExplosionResMult;

        public final ForgeConfigSpec.ConfigValue<Double> costMassScale;
        // Частицы (клиент): count/speed зависят от impactSpeed
        public final ForgeConfigSpec.ConfigValue<Double> blockBurstCountBase;
        public final ForgeConfigSpec.ConfigValue<Double> blockBurstCountPerSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> blockBurstSpeedBase;
        public final ForgeConfigSpec.ConfigValue<Double> blockBurstSpeedPerSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> maxBlockParticlesPerCollision;
        // Кастомные uck_*: count = base + impact*perSpeed, скорость статична
        public final ForgeConfigSpec.ConfigValue<Double> uckCountBase;
        public final ForgeConfigSpec.ConfigValue<Double> uckCountPerSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> uckSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> maxUckParticlesPerCollision;


        public final ForgeConfigSpec.ConfigValue<Double> shipSpeedL;

        public final ForgeConfigSpec.ConfigValue<Double> shipMass;

        public final ForgeConfigSpec.BooleanValue rayEnabled;

        public final ForgeConfigSpec.ConfigValue<Double> rayMinSpeed;

        public final ForgeConfigSpec.IntValue maxDepth;

        public final ForgeConfigSpec.BooleanValue radiusEnabled;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("general_ship-ship");

            minSpeed = builder
                    .comment("Мин. скорость")
                    .define("minSpeed", 9.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            minSpeedRadius = builder
                    .comment("Min радиус")
                    .define("minSpeedRadius", 80.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            sizeRadius = builder
                    .comment("радиус (только если radiusEnabled=true), 1 = 3x3x3 блока контакта")
                    .define("sizeRadius", 1, o -> o instanceof Integer i && i >= 0 && i <= 1000);

            builder.comment("\n");

            speedCollider = builder
                    .comment("использовать все точки контакта")
                    .define("speedCollider", 15.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            minSpeedExplosion = builder
                    .comment("Мин. скорость для взрыва")
                    .define("minSpeedExplosion", 120.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            explosionPower = builder
                    .comment("Мощность взрыва")
                    .define("explosionPower", 2.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);

            builder.comment("\n");

            explosionEnabled = builder
                    .comment("Взрыв вкл/выкл")
                    .define("explosionEnabled", true);

            builder.comment("\n");

            radiusEnabled = builder
                    .comment("Включить объёмный урон")
                    .define("radiusEnabled", true);

            builder.comment("\n");

            budgetPerSpeed = builder
                    .comment("Cost на 1м/с")
                    .define("budgetPerSpeed", 1.8, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.comment("\n");

            budgetUseMass = builder
                    .comment("зависимость бюджета от массы (чем тяжелее корабль, тем больше бюджет на разрушения).")
                    .define("budgetUseMass", true);

            builder.comment("\n");
/*
            filterWeakContacts = builder
                    .comment("Фильтровать слабые точки контакта по скорости")
                    .define("filterWeakContacts", false);
            weakContactSpeedFrac = builder
                    .comment("Порог отбора точек: cpSpeed >= maxCpSpeed * weakContactSpeedFrac")
                    .define("weakContactSpeedFrac", 0.2, o -> o instanceof Double d && d >= 0.0 && d <= 1.0);
*/
            costBase = builder
                    .comment("Min cost")
                    .define("costBase", 1.0, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.comment("\n");

            costExplosionResMult = builder
                    .comment("Множитель cost от toughness")
                    .define("costToughnessMult", 2.0, o -> o instanceof Double d && d >= 0.0 && d <= 1_000_000.0);

            builder.comment("\n");

            costMassScale = builder
                    .comment("Добавка к бюджету за 1 кг собственной массы")
                    .define("costMassScale", 0.005, o -> o instanceof Double d && d >= 0.0 && d <= 100.0);

            builder.pop();

            builder.push("particles");
            blockBurstCountBase = builder
                    .comment("частицы блоков: базовое количество")
                    .define("blockBurstCountBase", 5.0, o -> o instanceof Double d && d >= 0.0 && d <= 500.0);
            blockBurstCountPerSpeed = builder
                    .comment("добавка количества на 1 м/с")
                    .define("blockBurstCountPerSpeed", 0.4, o -> o instanceof Double d && d >= 0.0 && d <= 100.0);
            blockBurstSpeedBase = builder
                    .comment(" базовая скорость разлета")
                    .define("blockBurstSpeedBase", 0.6, o -> o instanceof Double d && d >= 0.0 && d <= 100.0);
            blockBurstSpeedPerSpeed = builder
                    .comment("добавка скорости на 1 м/с")
                    .define("blockBurstSpeedPerSpeed", 0.02, o -> o instanceof Double d && d >= 0.0 && d <= 10.0);
            maxBlockParticlesPerCollision = builder
                    .comment("максимальное кол-во частиц за один вызов")
                    .define("maxBlockParticlesPerCollision", 150.0, o -> o instanceof Double d && d >= 1.0 && d <= 5000.0);

            builder.comment("\n");
            uckCountBase = builder
                    .comment("Искры базовое кол-во")
                    .define("uckCountBase", 1.0, o -> o instanceof Double d && d >= 0.0 && d <= 100.0);
            uckCountPerSpeed = builder
                    .comment("добавка количества на 1 м/с")
                    .define("uckCountPerSpeed", 0.04, o -> o instanceof Double d && d >= 0.0 && d <= 10.0);
            uckSpeed = builder
                    .comment("статическая скорость разлета")
                    .define("uckSpeed", 0.23, o -> o instanceof Double d && d >= 0.0 && d <= 10.0);
            maxUckParticlesPerCollision = builder
                    .comment("максимальное кол-во частиц за один вызов")
                    .define("maxUckParticlesPerCollision", 150.0, o -> o instanceof Double d && d >= 1.0 && d <= 5000.0);

            builder.pop();

            builder.push("Depth_damage");
            shipSpeedL = builder
                    .comment("Блоков на 1 м/с")
                    .define("shipSpeedL", 0.016, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);
            builder.comment("\n");
            shipMass = builder
                    .comment("Блоков на 1 кг массы")
                    .define("shipMass", 0.0000013, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);
            builder.comment("\n");
            rayEnabled = builder
                    .comment("Включить \"глубокий\" урон ")
                    .define("rayEnabled", true);
            builder.comment("\n");
            rayMinSpeed = builder
                    .comment("Минимальная скорость")
                    .define("rayMinSpeed", 0.0, o -> o instanceof Double d && d >= 0.0 && d <= 1000.0);
            builder.comment("\n");
            maxDepth = builder
                    .comment("Максимальная \"Длина\" урона")
                    .defineInRange("Max_Depth", 10, 1, 1000);
            builder.pop();
        }
    }

    public static final class VisualClientConfig {
        public final ForgeConfigSpec.BooleanValue disableCollisionParticles;

        VisualClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("Client");
            disableCollisionParticles = builder
                    .comment("отключить отрисовку частиц")
                    .define("disableCollisionParticles", false);
            builder.pop();
        }
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new org.collisionmod.collision.forge.VSCollisionConfig.Common(builder);
        COMMON_SPEC = builder.build();
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new VisualClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }
}