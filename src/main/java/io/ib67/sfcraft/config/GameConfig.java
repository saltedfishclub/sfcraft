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
    public Gps gps = new Gps();
    public StandingFirmTotem standingFirmTotem = new StandingFirmTotem();
    public MapArt mapArt = new MapArt();

    public static class MapArt {
        // 单张图片的下载体积上限(字节)
        public int maxDownloadBytes = 8388608;
        // HTTP 超时(秒)
        public int httpTimeoutSeconds = 15;
        // 输入图片单边像素上限(解码像素前按头部尺寸拦截,防爆内存);无论原图多大,成品恒为 128x128 一张地图
        public int maxImageDimension = 2048;
        // 远端下载/解码失败的 URL 冷却秒数,期间同一地址不再发起请求
        public int failureCooldownSeconds = 60;
        // 铁砧生成地图画的经验等级花费
        public int anvilXpCost = 1;
    }

    public static class StandingFirmTotem {
        // 可反复使用的图腾变种:触发时扣掉一部分总经验,并进入冷却
        public int cooldownTicks = 600; // 30 秒
        // 总经验点数低于此值时放弃触发(直接让玩家死亡);160 = 等级 10
        public int minExperiencePoints = 160;
        public double experienceDrainRatio = 0.5;
    }

    public static class Gps {
        // 视线与目标方向的夹角小于该值(度)时,光标锁定在准星上;超过则跳到真实目标方向
        public double toleranceDegrees = 12.0;
        // 光标悬浮在视线前方的距离;放远一点航向偏差在画面上更明显,也更不容易糊在脸上
        public double cursorDistance = 8.0;
        // 鞘翅飞行时改用的光标距离:高速飞行下虚拟实体位置插值滞后,光标会"跟不上",拉远以抵消
        public double flyingCursorDistance = 20.0;
        public double arriveRadius = 4.0;
        // 水平(X-Z)距离大于该值时进入远距离巡航:光标锁在眼睛所在水平面,只随左右转向(yaw)偏移、
        // 忽略俯仰,这样用鞘翅高速飞行低头看地面时光标不会跟着往下沉;近于此值则恢复全向指示
        public double planarDistance = 50.0;
    }

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
        public int maxRecipients = 3;
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
