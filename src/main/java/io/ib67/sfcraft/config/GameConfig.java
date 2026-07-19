package io.ib67.sfcraft.config;

/**
 * sfcraft 游戏内容的数值配置,位于 sfcraft/gameplay.json(与 config.json 同目录)。
 * 加载/重载由 {@link GameConfigService} 负责;/sfcraft reload 可热重载。
 * 注意:战利品注入(loot)的数值在数据包加载时取样,改完还需再执行原版 /reload 才生效。
 */
public class GameConfig {
    public Bomb bomb = new Bomb();
    public ReverseToken reverseToken = new ReverseToken();
    public Cauldron cauldron = new Cauldron();
    public GravityCrystal gravityCrystal = new GravityCrystal();
    public Loot loot = new Loot();
    public Beheading beheading = new Beheading();
    public Commander commander = new Commander();
    public Mount mount = new Mount();

    public static class Mount {
        // 驯服食物(需怪物处于虚弱、周围无掠夺者、非袭击时喂食);默认金苹果,呼应虚弱+金苹果治疗语义
        public String ravagerTameFood = "minecraft:golden_apple";
        public String hoglinTameFood = "minecraft:golden_apple";
        public float tameSuccessChance = 0.2F;
        public boolean requireSaddleToRide = true;
        public double tameNearbyRaiderRadius = 12.0;
        public double rideSpeedFactor = 1.15;
        public RavagerDash ravagerDash = new RavagerDash();

        public static class RavagerDash {
            public int boostTicks = 12;
            public double speedMultiplier = 2.2;
            public int cooldownTicks = 40;
            public int durabilityCost = 1;
            public double collisionDamage = 6.0;
        }
    }

    public static class Commander {
        // 新生成的敌怪(Enemy)自然刷新时携带「统帅」效果的概率
        public double spawnChance = 0.02;
        public double radius = 12.0;
        public int maxRecipients = 2;
        public int buffDurationTicks = 60;
        public int glowRefreshTicks = 40;
        public int scanIntervalTicks = 10;
        public int particleIntervalTicks = 5;
        public double cohesionMaxDistance = 6.0;
        public double cohesionSpeed = 1.2;
    }

    public static class Beheading {
        // 掉头概率 = baseChance + perLevelChance * 附魔等级(玩家头颅单独用 playerHeadChance)
        public float baseChance = 0.05F;
        public float perLevelChance = 0.05F;
        public float playerHeadChance = 0.05F;
    }

    public static class Bomb {
        public int fuseTicks = 80;
        public float explosionPower = 3F;
        public int throwCooldownTicks = 10;
        public double slowCircleRadius = 2.5;
        public int slownessAmplifier = 2;
        public int blazeIgniteTicks = 100;
        public double blazeIgniteRadius = 3.0;
        public int blazeFireSpreadMin = 2;
        public int blazeFireSpreadMax = 3;
        // 烟雾:飞行时朝速度反方向、落地后朝落地面法线喷黑色粒子
        public int smokeColor = 0x000000;
        public int smokeParticlesPerTick = 3;
        public float smokeParticleScale = 1.4F;
        public double smokeSpeed = 0.05;
    }

    public static class ReverseToken {
        public int maxUses = 8;
        public int cooldownSeconds = 300;
    }

    public static class Cauldron {
        public int maxItems = 4;
        public int reactionTicks = 100;
    }

    public static class GravityCrystal {
        public double range = 8.0;
        public int boostIntervalTicks = 2;
        public double[] boostPerCharge = {0.0, 0.15, 0.20, 0.25, 0.30};
        public double maxSpeedMetersPerSecond = 8.0;
    }

    public static class Loot {
        public float dungeonBombChance = 0.3F;
        public float dungeonObsidianBombChance = 0.15F;
        public float creeperBombChance = 0.1F;
    }
}
