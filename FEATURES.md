# SFCraft 游戏特性总览(Wiki 生成源)

> **本文档的用途**：作为 SFCraft `new-feat` 分支所有游戏玩法内容的**单一事实来源**，供下游 agent 据此生成面向玩家的详细 Wiki 页面。
>
> **使用约定**：
> - 每个二级/三级标题的特性小节，都足以独立展开成一个 Wiki 页面。
> - 所有数值取自 `GameConfig.java` 的默认值（对应运行目录下 `sfcraft/gameplay.json`），管理员可在该文件中修改，多数支持 `/sfcraft reload` 热重载。
> - 中文物品/特性名取自 `assets/sfcraft/lang/zh_cn.json`；英文名取自 `en_us.json`。
> - “实现参考”小节面向开发者/进阶 Wiki，玩家向 Wiki 可略去。
> - 本文档描述**设计意图与当前行为**，不含待修复缺陷；已知问题另行在代码审计中跟踪。

---

## 关于 SFCraft

SFCraft 是一个 **纯服务端** Fabric mod（Minecraft 26.2）。所有自定义方块、物品、实体、药水效果都通过 [Polymer](https://polymer.pb4.eu) 伪装成原版内容——**玩家使用原版客户端 + 服务器下发的材质包**即可游玩，无需安装任何 mod。

因此：
- 自定义方块/物品在客户端看到的是某个“替身”原版外观（如炼药锅、重生锚、雪球），实际行为由服务端裁决。
- 玩家可见的文字通过翻译键（translate key）下发，配合材质包里的语言文件显示为中文/英文。

---

## 特性总览

| 特性 | 类型 | 获取途径 | 一句话 |
|---|---|---|---|
| 紫水晶炼药锅 | 方块 | 合成胚体 → 熔炼 | 把材料丢进锅里自动炼制的多用途炼药方块 |
| 重力水晶 | 方块 | 炼药锅炼制 | 充能后加速范围内移动生物的水晶 |
| 炸弹 | 物品 | 战利品/村民交易 | 右键投掷、只炸方块不毁地形的基础炸弹 |
| 黑曜石炸弹 | 物品 | 合成/战利品 | 落地释放减速力场后爆炸 |
| 烈焰炸弹 | 物品 | 合成 | 爆炸点燃并撒火 |
| 珍珠信物 | 物品 | 合成 | 绑定自己，他人使用可把你召唤过去 |
| 反向珍珠信物 | 物品 | 炼药锅炼制 | 把自己传送到绑定者身边，有次数和冷却 |
| 午餐盒 | 物品 | 合成 | 随身 9 格食物容器，右键即食 |
| 屹立不倒图腾 | 物品 | 炼药锅炼制 | 可反复使用、消耗经验的不死图腾变种 |
| 斩首 | 附魔 | 附魔台 / 图书管理员 | 击杀生物概率掉落其头颅 |
| 统帅 | 效果 | 自然刷怪携带 | 敌怪首领为周围怪物提供增益并统率跟随 |
| 劫掠兽/疣猪兽坐骑 | 生物 | 驯服 | 喂食驯服可骑乘，劫掠兽可冲刺 |
| GPS 导航 | 指令 | `/gps` | 视线前方的发光光标指引你前往坐标 |
| 配置热重载 | 指令 | `/sfcraft reload` | 重载 gameplay 配置 |

---

## 物品与方块特性

### 紫水晶炼药锅（Amethyst Cauldron）

**简介**：一台会发光、冒萤火虫粒子的自动炼药方块。它是 SFCraft 多种高级道具的合成核心。

**玩法**：
- **投料**：把配方所需的物品**以掉落物形式丢进锅里**（不是手持右键）。物品被吸入后固定在锅中，最多容纳 4 件，并以悬浮小物品的形式展示在锅口。
- **加水**：手持水桶右键锅可注水（部分配方需要）。
- **反应条件**：当锅内物品凑齐某个配方，且满足该配方的**水 / 加热**条件时，反应自动开始，锅上浮现对应粒子；反应结束后产物从锅上方弹出。凑齐配方但缺条件时会冒烟提示。
- **加热来源**：锅正下方为火、灵魂火、岩浆块或岩浆（含流动岩浆）。
- **取回原料**：**空手右键**炼药锅可取回最后放入的一件原料（后进先出）。反应进行中禁止取出（有屏幕提示）。
- **破坏**：普通破坏掉落铁锭 + 紫水晶碎片；**精准采集**才能完整挖回炼药锅本身。

**获取**：
1. 合成**紫水晶炼药锅胚体**：`铁锭 ×7`（外圈）+ `紫水晶块 ×1`（中心），有序合成
   ```
   铁 空 铁
   铁 晶 铁
   铁 铁 铁
   ```
2. 将胚体**熔炼**（熔炉，约 10 秒，经验 0.5）得到成品紫水晶炼药锅。

**炼药锅配方系统**：炼药锅本身不写死配方，任何特性都能向它注册“投入物（含催化剂）+ 水/加热条件 → 产物”的反应。当前已注册的配方见 [反向珍珠信物](#反向珍珠信物reverse-pearl-token)、[屹立不倒图腾](#屹立不倒图腾standing-firm-totem) 与 [重力水晶](#重力水晶gravity-crystal)。

**可配置项**（`cauldron`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `maxItems` | 4 | 锅内最多容纳的物品数 |
| `reactionTicks` | 100 | 默认反应时长（tick），被各配方引用 |

**实现参考**：`module.game.cauldron.CauldronModule`；方块 `AmethystCauldronBlock`（客户端伪装 `minecraft:cauldron`，有水时为 `water_cauldron` LEVEL=3），方块实体 `AmethystCauldronBlockEntity`，悬浮展示 `CauldronDisplayHolder`。物品 id `sfcraft:amethyst_cauldron`、`sfcraft:amethyst_cauldron_blank`。配方注册表 `registry.CauldronRecipeRegistry`。

---

### 重力水晶（Gravity Crystal）

**简介**：一块可用紫水晶碎片充能的水晶，充能后会加速周围**正在移动**的生物。

**玩法**：
- **充能**：手持紫水晶碎片右键，充能等级 0→4（外观伪装成重生锚，充能越高发光越亮）。
- **加速**：充能后每 2 tick 给范围内移动中的生物的速度矢量乘以一个系数（按充能等级递增），并有向水晶汇聚的粒子。已达速度上限的实体不再被加速（不会拖慢已在高速的玩家）。

**获取**：**炼药锅炼制** —— 把 `羽毛 + 重生锚 + 回响碎片（催化剂）` 丢进紫水晶炼药锅，需**加热**（不需水）。三样投入物均被消耗，反应完成后弹出一块重力水晶。破坏时掉落自身。

**可配置项**（`gravityCrystal`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `range` | 8.0 | 作用半径（格） |
| `boostIntervalTicks` | 2 | 加速判定间隔（tick） |
| `boostPerCharge` | `{0.0, 0.15, 0.20, 0.25, 0.30}` | 按充能等级（数组下标）的加速比 |
| `maxSpeedMetersPerSecond` | 8.0 | 加速上限（米/秒） |

**实现参考**：`module.game.crystal.GravityCrystalModule`；`GravityCrystalBlock`（伪装 `minecraft:respawn_anchor`，复用其 CHARGE 状态）、`GravityCrystalBlockEntity`。id `sfcraft:gravity_crystal`。炼药锅配方在 `/sfcraft reload` 时重建以应用新的 `reactionTicks`，反应粒子为 `REVERSE_PORTAL`。

---

### 炸弹（Bomb）/ 黑曜石炸弹（Obsidian Bomb）/ 烈焰炸弹（Blaze Bomb）

**简介**：三种右键投掷的炸弹，飞行时伪装成雪球，穿过实体、只在**撞到方块**或**引信到期**时爆炸。默认爆炸**不破坏地形**（仍伤害实体）。

**各变种玩法**：
- **炸弹**：基础爆炸。
- **黑曜石炸弹**：落地后每 10 tick 对半径 2.5 内的生物施加缓速（脚下浮现灵魂火焰圈），引信到期后爆炸。
- **烈焰炸弹**：爆炸时点燃半径内生物并随机撒下火焰。

**获取**：

| 物品 | 获取途径 |
|---|---|
| 炸弹 | 地牢箱 30%（1–3 个）；苦力怕掉落 10%；武器匠村民 4 级交易：`4 火焰弹 + 1–8 绿宝石 → 4 炸弹`（最多 12 次） |
| 黑曜石炸弹 | 合成 `8 炸弹 + 1 哭泣的黑曜石 → 8 个`；另地牢箱 15%（1–2 个） |
| 烈焰炸弹 | 合成 `1 炸弹 + 1 烈焰粉 → 1 个` |

> 战利品概率在数据包加载时取样，修改配置后需先 `/sfcraft reload` 再执行原版 `/reload` 才生效。

**可配置项**（`bomb` 与 `loot`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `bomb.fuseTicks` | 80 | 引信时长 |
| `bomb.explosionPower` | 3.0 | 爆炸威力 |
| `bomb.throwCooldownTicks` | 10 | 投掷冷却 |
| `bomb.slowCircleRadius` | 2.5 | 黑曜石炸弹减速半径 |
| `bomb.slownessAmplifier` | 2 | 减速等级 |
| `bomb.blazeIgniteTicks` | 100 | 烈焰炸弹点燃时长 |
| `bomb.blazeIgniteRadius` | 3.0 | 烈焰炸弹点燃半径 |
| `bomb.blazeFireSpreadMin`/`Max` | 2 / 3 | 烈焰炸弹撒火数量范围 |
| `bomb.smokeColor` | `0x000000` | 烟雾颜色 |
| `bomb.smokeParticlesPerTick` | 3 | 每 tick 烟雾粒子数 |
| `bomb.smokeParticleScale` | 1.4 | 烟雾粒子大小 |
| `bomb.smokeSpeed` | 0.05 | 烟雾扩散速度 |
| `loot.dungeonBombChance` | 0.3 | 地牢箱炸弹概率 |
| `loot.dungeonObsidianBombChance` | 0.15 | 地牢箱黑曜石炸弹概率 |
| `loot.creeperBombChance` | 0.1 | 苦力怕掉落炸弹概率 |

**实现参考**：`module.game.bomb.BombModule`、`BombItem`（伪装 `warped_fungus_on_a_stick`）、`BombEntity`（伪装 `snowball`）。物品 id `sfcraft:bomb` / `obsidian_bomb` / `blaze_bomb`，实体 id `sfcraft:bomb`。

---

### 珍珠信物（Pearl Token）

**简介**：一枚可绑定持有者的信物，被他人使用时会把绑定者召唤过去。

**玩法**：
- **绑定**：右键**绑定自己**（记录你的名字，物品附上“主人”标签）。
- **召唤**：**他人**右键已绑定的信物，会把**主人召唤到使用者身边**（主人须在线，可跨维度），消耗 1 个信物。主人离线时提示失败。

**获取**：无序合成 `末影珍珠 + 紫水晶碎片`。

**实现参考**：`module.game.token.TokenModule`、`PearlTokenItem`（伪装 `warped_fungus_on_a_stick`），持有者信息存于 `minecraft:custom_data`。id `sfcraft:pearl_token`。

---

### 反向珍珠信物（Reverse Pearl Token）

**简介**：与珍珠信物方向相反——把**自己传送到绑定者身边**。有使用次数和冷却。

**玩法**：
- 右键把自己传送到绑定主人所在位置（主人须在线）。
- 有**冷却时间**（冷却记录随物品存档，重新登录仍有效）。
- 使用次数由**耐久**承载（客户端显示为鱼竿的耐久条），耗尽后化为灰烬。

**获取**：**炼药锅炼制** —— 把 `已绑定的珍珠信物 + 回响碎片（催化剂）` 丢进紫水晶炼药锅，需**水 + 加热**。

**可配置项**（`reverseToken`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `maxUses` | 8 | 使用次数（注册时写入耐久，**不可热重载**，改后需重启） |
| `cooldownSeconds` | 300 | 每次使用的冷却秒数 |

**实现参考**：`ReversePearlTokenItem`（伪装 `fishing_rod`）。id `sfcraft:reverse_pearl_token`。配方在 `/sfcraft reload` 时重建以应用新的 `reactionTicks`。

---

### 午餐盒（Lunch Box）

**简介**：一个随身携带的 9 格食物容器，右键即可直接进食。

**玩法**：
- **右键进食**：直接吃盒内第一份当前可吃的食物，播放完整的原版进食动画，并应用该食物的营养/饱和/效果，以及余留物（如空碗、空瓶留回盒内）。空盒或不饿时有屏幕提示。
- **Shift + 右键**：打开 9 格容器界面，只能放入带食物属性的物品（不能套娃放午餐盒）。

**获取**：有序合成 —— 4 铁锭十字环绕 1 个收纳袋（原版 `minecraft:bundle`），中心为收纳袋：
   ```
   空 铁 空
   铁 袋 铁
   空 铁 空
   ```

**实现参考**：`module.game.lunchbox.LunchBoxModule`、`LunchBoxItem`（伪装 `bowl`）、`LunchBoxMenu`（GENERIC_9x1 界面）。盒内容物存于 `minecraft:container` 组件。id `sfcraft:lunch_box`。

---

### 屹立不倒图腾（Standing Firm）

**简介**：一个**可反复使用**的不死图腾变种，用消耗经验换取一次次的死里逃生。

**玩法**：
- 受到致死伤害时，若满足条件则触发保护：**扣掉你当前一半的总经验**、回血、清除负面效果并给予图腾同款增益、播放图腾动画——且**图腾本身不消耗**，之后进入 30 秒冷却。触发时有系统消息提示。
- **不触发的情况**：
  - **冷却中**或**总经验不足 160 点（10 级）**→ 放弃保护，玩家正常死亡（图腾仍不消耗）。
  - **穿透无敌的伤害**（虚空、`/kill` 等）→ 不救。
- 图腾放在主手或副手皆可触发。

**获取**：**炼药锅炼制** —— 把 `不死图腾 + 附魔之瓶 + 绿宝石（催化剂）` 丢进紫水晶炼药锅，需**加热**（不需水）。

**可配置项**（`standingFirmTotem`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `cooldownTicks` | 600 | 触发后的冷却（tick，600 = 30 秒） |
| `minExperiencePoints` | 160 | 触发所需的最低总经验点数（160 = 10 级） |
| `experienceDrainRatio` | 0.5 | 触发时扣除的总经验比例 |

**实现参考**：`module.game.totem.TotemModule`（`tryProtect` 逻辑）、`ExperienceHelper`（按等级+进度条精确计算总经验）、mixin `TotemProtectionMixin`（接管原版 `checkTotemDeathProtection`）。物品自带原版死亡保护组件 `DEATH_PROTECTION`。id `sfcraft:standing_firm_totem`。配方在 `/sfcraft reload` 时重建。

---

## 附魔与效果

### 斩首（Beheading）

**简介**：一个剑类附魔，击杀生物时有概率掉落其头颅。

**玩法**：主手武器带此附魔击杀生物时，按概率掉头：
- 支持原版有头颅的生物：僵尸、骷髅、凋灵骷髅、苦力怕、猪灵、猪灵蛮兵、僵尸猪灵、末影龙 → 对应头颅。
- **玩家 → 带其皮肤的玩家头**。
- 掉落概率：普通生物 = `baseChance + perLevelChance × 附魔等级`；玩家 = 固定 `playerHeadChance`（不随等级）。

**获取**：数据驱动附魔，最高 3 级，适用剑类物品，可在附魔台获得，也可从**图书管理员村民**处购买附魔书（已加入 `#minecraft:tradeable` 标签，因此会出现在其附魔书交易池中）。

**可配置项**（`beheading`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `baseChance` | 0.05 | 基础掉头概率 |
| `perLevelChance` | 0.05 | 每级附加概率 |
| `playerHeadChance` | 0.05 | 玩家头颅概率（固定） |

**实现参考**：`module.game.beheading.BeheadingModule`（监听死亡事件）；附魔定义 `data/sfcraft/enchantment/beheading.json`，ResourceKey `sfcraft:beheading`；村民可售由 `data/minecraft/tags/enchantment/tradeable.json` 将其加入原版 `#minecraft:tradeable` 标签实现（原版据此标签生成图书管理员的附魔书交易）。

---

### 统帅（Commander）

**简介**：少数自然刷新的敌怪会成为“首领”，为周围怪物提供增益并统率它们协同作战。

**玩法**：
- 自然刷新的敌怪有小概率携带「统帅」效果，携带者身上发出紫色光芒（发光 + 浅紫队伍色）。
- 首领为半径内至多 N 个「与其攻击同一目标、且自身没有统帅效果」的怪物提供 2 种增益（从力量/抗性/迅捷中稳定地选 2 种）。
- 首领与受益者之间有紫色粒子纽带；受益者离得太远会被寻路拉回。
- 首领切换攻击目标时，整队同步跟随新目标。
- 首领死亡或失去效果时，立即撤销增益、消除发光、退出队伍。

**获取**：非物品，自然刷怪时随机附加。

**可配置项**（`commander`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `spawnChance` | 0.02 | 自然刷新敌怪携带统帅的概率 |
| `radius` | 12.0 | 增益作用半径 |
| `maxRecipients` | 2 | 最多受益怪物数 |
| `buffDurationTicks` | 60 | 增益持续时间 |
| `glowRefreshTicks` | 40 | 发光刷新间隔 |
| `scanIntervalTicks` | 10 | 扫描间隔 |
| `particleIntervalTicks` | 5 | 粒子纽带刷新间隔 |
| `cohesionMaxDistance` | 6.0 | 超过此距离触发寻路拉回 |
| `cohesionSpeed` | 1.2 | 拉回移动速度 |

**实现参考**：`module.game.commander.CommanderModule`、`CommanderMobEffect`（客户端替身为发光效果）。效果 id `sfcraft:commander`；携带入口 mixin `MobCommanderMixin`（仅自然刷新的敌对生物）。

---

## 生物与坐骑

### 劫掠兽 / 疣猪兽坐骑（Ravager / Hoglin Mount）

**简介**：可以驯服劫掠兽和疣猪兽作为坐骑，劫掠兽还能冲刺撞击。

**玩法**：
- **驯服**：对满足条件的劫掠兽/疣猪兽喂食（默认金苹果）有概率驯服。前置条件：目标处于**虚弱**状态、附近无掠夺者、非袭击进行中。喂食会消耗食物，成功/失败均有粒子与音效反馈。
- **归属**：驯服后仅主人可交互；驯服后的坐骑不再攻击任何玩家。
- **上鞍与骑乘**：主人手持鞍右键上鞍；空手右键骑乘（默认需已上鞍）。骑乘为**服务端权威转向**（读取玩家输入包），骑乘时清除坐骑仇恨。
- **劫掠兽冲刺**：骑乘劫掠兽时手持胡萝卜钓竿可冲刺——短时间强制全速前进并提速，冲刺中撞到的生物受到一次性撞击伤害；有冷却并消耗钓竿耐久。

**获取**：无物品，驯服世界中的原版劫掠兽/疣猪兽。

**可配置项**（`mount`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `ravagerTameFood` | `minecraft:golden_apple` | 劫掠兽驯服食物 |
| `hoglinTameFood` | `minecraft:golden_apple` | 疣猪兽驯服食物 |
| `tameSuccessChance` | 0.2 | 单次喂食驯服成功率 |
| `requireSaddleToRide` | true | 骑乘是否需要先上鞍 |
| `tameNearbyRaiderRadius` | 12.0 | 判定“附近掠夺者”的半径 |
| `rideSpeedFactor` | 1.15 | 骑乘移速系数 |
| `ravagerDash.boostTicks` | 12 | 冲刺持续 tick |
| `ravagerDash.speedMultiplier` | 2.2 | 冲刺提速倍率 |
| `ravagerDash.cooldownTicks` | 40 | 冲刺冷却 |
| `ravagerDash.durabilityCost` | 1 | 冲刺消耗钓竿耐久 |
| `ravagerDash.collisionDamage` | 6.0 | 冲刺撞击伤害 |

**实现参考**：`module.game.mount.MountModule`（交互入口）、`MountLogic`（共享逻辑）、`MountAccess`（存 owner/上鞍/冲刺状态的鸭子接口）；mixin `RavagerMountMixin`、`HoglinMountMixin`（覆写骑乘相关方法）。归属与上鞍状态存于实体存档。

---

## 指令

### `/gps <x> <y> <z>` — 坐标导航

**简介**：在你的视线前方悬浮一只**只有你能看到的发光小史莱姆**作为导航光标，指引你前往指定坐标。

**玩法**：
- `/gps <x> <y> <z>` 开始导航（支持 `~` 相对坐标）。光标是纯服务端虚拟实体：别人看不到、无实体碰撞、发光轮廓可穿墙显示，即使光标位置进入地形你也能看到方向。
- **对准机制**：当你的视线方向与目标方向的偏差在容差角内时，光标粘在准星上（画面上纹丝不动）；一旦偏离超过容差，光标会跳到真实的目标方向，提示你把准星转回光标上以校正航向。
- **远近两段**：距目标的水平距离较远时（巡航段），光标锁定在你眼睛所在的水平面、只随左右转向（yaw）偏移、忽略俯仰——因此用鞘翅高速飞行低头看地面时光标不会乱跑；接近目标时恢复含俯仰的全向指示，便于对准上下方的目标。
- **自动结束**：到达目标半径内、切换维度、下线、死亡重生均会正确处理（重生后重新对客户端下发光标）。
- `/gps stop` 手动取消导航。
- 一名玩家同时只有一个导航目标，重复 `/gps` 会替换目标。

**权限**：任意玩家可用（无权限等级要求）。

**可配置项**（`gps`）：

| 字段 | 默认 | 含义 |
|---|---|---|
| `toleranceDegrees` | 12.0 | 视线对准的容差角（度） |
| `cursorDistance` | 8.0 | 光标悬浮在视线前方的距离 |
| `arriveRadius` | 4.0 | 到达判定半径 |
| `planarDistance` | 50.0 | 水平距离大于此值时进入“只随左右转向”的巡航模式 |

**实现参考**：`module.command.GpsModule`；光标为 polymer 虚拟实体 `SimpleEntityElement(SLIME)` + `ManualAttachment`，位置每服务器 tick 按玩家最新位置重算。

---

### `/sfcraft reload` — 配置热重载

**简介**：重载 SFCraft 的 gameplay 配置。

**玩法/用途**：
- 触发 `SFConfigReload` 事件，各监听模块随之重读配置：
  - 重新加载 `sfcraft/gameplay.json`（数值配置）。
  - 重建炼药锅配方（应用新的反应时长等）。
  - 重读 MOTD 等资源配置。
- **注意**：战利品概率、以及注册时写入的耐久类数值（如反向信物使用次数）不随此指令生效——前者还需原版 `/reload`，后者需重启。
- 成功与失败都会返回翻译文本反馈。

**权限**：`Commands.LEVEL_GAMEMASTERS`（管理员/命令方块等级）。

**实现参考**：`module.command.ReloadCommandModule`、回调 `callback.SFConfigReload`。

---

## 配置文件（`sfcraft/gameplay.json`）

游戏内容的数值配置集中在运行目录下的 `sfcraft/gameplay.json`（与 `config.json` 同目录），文件不存在时自动写出默认值。结构与默认值（节选，完整默认值见上文各特性表）：

```
bomb              炸弹三兄弟的引信/威力/烟雾/减速/点燃参数
reverseToken      反向珍珠信物：使用次数、冷却
cauldron          炼药锅：容量、默认反应时长
gravityCrystal    重力水晶：范围、各充能加速比、速度上限
loot              炸弹类战利品注入概率
beheading         斩首附魔掉头概率
commander         统帅效果：概率、半径、增益、寻路等
mount             坐骑：驯服食物/成功率/骑乘/冲刺参数
gps               GPS 导航：容差角、光标距离、到达半径、巡航阈值
standingFirmTotem 屹立不倒图腾：冷却、经验门槛、经验扣除比例
```

热重载说明：
- **可热重载**（`/sfcraft reload` 即时生效）：多数数值参数。
- **需原版 `/reload`**：战利品概率（在数据包加载时取样）。
- **需重启**：注册时写入物品组件的值（如反向信物耐久 `reverseToken.maxUses`）。

---

## 通用机制

### Polymer 伪装

所有自定义内容对原版客户端呈现为某个“替身”原版对象；玩家看到的自定义外观来自服务器下发的材质包（通过替身物品的模型覆盖实现）。因此：
- 自定义方块/物品在世界与背包中的外观由材质包决定，逻辑由服务端裁决。
- 自定义药水效果（如统帅）对客户端替换为一个原版效果（发光），避免未知 id 导致客户端异常。

### 配置热重载

`/sfcraft reload` → `SFConfigReload` 事件 → 各模块重读配置（详见上文）。

### 多语言 / 材质包

玩家可见文本一律使用翻译键，语言文件位于 `assets/sfcraft/lang/zh_cn.json` 与 `en_us.json`，随服务器材质包下发，由客户端按其语言解析。物品显示名通过物品组件挂载翻译键。
