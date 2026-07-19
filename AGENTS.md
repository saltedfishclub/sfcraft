# sfcraft 开发指南

Fabric **纯服务端** mod(`fabric.mod.json` 中 `environment: "server"`),Minecraft **26.2**,Java 25,**官方 Mojang 映射**(accesswidener 头为 `v2 official`)。所有自定义内容通过 [Polymer](https://polymer.pb4.eu) 伪装成原版内容,原版客户端可直接游玩。

## 构建与验证

仓库**没有** gradlew wrapper,使用系统 gradle + sdkman JDK:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/25.0.1-amzn
~/.sdkman/candidates/gradle/9.5.1/bin/gradle compileJava --console=plain   # 编译(mixin AP 会校验注入目标)
~/.sdkman/candidates/gradle/9.5.1/bin/gradle runServer --console=plain    # dev 服务器(run/ 目录,EULA 已接受)
```

查 26.2 真实签名(解决 cannot find symbol 的最快办法):

```bash
javap -cp ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar <类全名>
```

## 包结构

代码分两个顶层包:

- `io.ib67.sfcraft` — 服务器功能模块(Guice DI + `ServerModule` 生命周期体系,入口 `SFCraftInitializer`)。mixin 全部在 `io.ib67.sfcraft.mixin`(配置 `sfcraft.mixins.json`)。
- `sfcraft` — **Polymer 游戏内容包**(本文档主题),由 `GameExtensionModule`(一个 `ServerModule`)在 ModInit 阶段接入,初始化顺序固定:config → blocks → block entities → items → entities → 炼药锅配方 → 战利品注入 → 数据重载监听 → 命令。

### sfcraft 包内容

| 位置 | 内容 |
|---|---|
| `SFBlocks` / `SFItems` / `SFBlockEntities` / `SFEntities` | 注册表。helper 模式:`ResourceKey.create` + `properties.setId(key)` + `Registry.register`。BE 类型必须 `PolymerBlockUtils.registerBlockEntity`,实体类型必须 `PolymerEntityUtils.registerType` |
| `GameConfig` | 数值配置,读写 `sfcraft/gameplay.json`(运行目录下,不存在时写出默认值)。代码里用 `GameConfig.get().xxx` 每次现读,不要缓存 |
| `blocks/` | 紫水晶炼药锅(`PolymerBlock` + `BlockWithElementHolder`,BE 吸收掉落物、催化反应)、重力水晶(伪装重生锚,速度矢量加速)、`CauldronDisplayHolder`(polymer-virtual-entity 物品悬浮展示) |
| `cauldron/` | 代码内配方注册表 `CauldronRecipes`(谓词匹配 + 催化剂 + 水/加热条件) |
| `entity/BombEntity` | 三种炸弹共用实体,伪装成雪球;引信从抛出开始计时,只与方块碰撞(`canHitEntity` 恒 false) |
| `items/` | 炸弹、珍珠信物/反向信物(owner 存 `minecraft:custom_data`)、午餐盒(`food`+`consumable` 组件驱动原版进食动画,内容存 `minecraft:container` 组件,`LunchBoxMenu` 为 GENERIC_9x1) |
| `zombie/` | 数据驱动僵尸 class(`SimpleJsonResourceReloadListener`),配合 `ZombieMixin` 注入 `finalizeSpawn` |

### 数据驱动内容(resources)

- `data/sfcraft/recipe/` — 合成/熔炼配方(26.2 单数目录名)
- `data/sfcraft/loot_table/blocks/` — 方块战利品表(精准采集用 `alternatives` + `match_tool`)
- `data/sfcraft/villager_trade/weaponsmith/4/` + `data/minecraft/tags/villager_trade/weaponsmith/level_4.json` — 26.2 村民交易是数据驱动的:交易 JSON + tag 合并进对应职业/等级池;`TradeCost.count` 是 NumberProvider,支持 `minecraft:uniform` 随机
- `data/sfcraft/zombie_class/` — 僵尸 class(字段:`min_level`/`max_level`/`weight`/`name`/`attributes`/`equipment`)
- `data/minecraft/tags/block/mineable/pickaxe.json` — 自定义方块的挖掘工具 tag

### 重载

- `/sfcraft reload`(权限 LEVEL_GAMEMASTERS)— 重载 `sfcraft/gameplay.json` 并重建炼药锅配方
- 原版 `/reload` — 重载数据包内容(僵尸 class、配方、战利品表、村民交易)
- **战利品概率**在战利品表构建时取样:改 gameplay.json 里的 loot 数值要先 `/sfcraft reload` 再 `/reload`

## 注意事项(踩过的坑)

**26.2 Mojmap 重命名**(相对 1.21.x,写代码前先 javap 确认):
- `ResourceLocation` → `net.minecraft.resources.Identifier`(`Identifier.fromNamespaceAndPath`)
- EntityType 常量移到 `net.minecraft.world.entity.EntityTypes`(`EntityTypes.SNOWBALL`)
- `Level.isClientSide` 字段私有化 → 用 `isClientSide()`;`Level.random` → `getRandom()`
- BE 存档:`saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`,列表用 `out.list(name, Codec)` / `in.listOrEmpty(...)`
- `BlockBehaviour.onRemove` 已删 → 覆写 `BlockEntity.preRemoveSideEffects`
- 类移包:`Zombie` → `entity.monster.zombie`,`ThrowableItemProjectile` → `entity.projectile.throwableitemprojectile`,`VillagerProfession` → `entity.npc.villager`
- `MobEffects.SLOWNESS`(不是 MOVEMENT_SLOWDOWN);`SimpleContainer` 没有 addListener 了(覆写 `setChanged()` 持久化)
- 装备掉落率是 `Mob` 私有字段 `dropChances`(`DropChances` record),用 `MobAccessor` mixin 写入

**Polymer(0.17.3+26.2)**:
- `PacketContext` 已迁到 `net.fabricmc.fabric.api.networking.v1.context.PacketContext`
- 伪装物品尽量选**非方块、无自定义 use 行为**的原版物品:方块物品有放置鬼影;有 use 覆写的物品(如诡异菌钓竿的 `FoodOnAStickItem`)会吃掉客户端的 consumable 预测(进食动画不显示)
- 要让客户端拿到额外组件(如 `food`/`consumable`),覆写 `getPolymerItemStack` 显式复制
- 物品中文名用 `Properties.component(DataComponents.ITEM_NAME, Component.literal(...))`(服务端 mod 没有 lang 文件)

**加载时序**:
- 初始数据包加载**早于物品组件初始化**,自定义 Codec 里不能用 `ItemStack.CODEC`(报 "Item ... does not have components yet")——只存物品 id(`BuiltInRegistries.ITEM.holderByNameCodec()`),用的时候再 `new ItemStack(holder)`
- 注册必须发生在 ModInit(`GameExtensionModule.onInitialize`),`ServerModule.onEnable` 阶段注册表已冻结

**玩家移动是客户端权威**:服务端改玩家速度矢量要置 `entity.hurtMarked = true` 触发 motion 包,且体感可能有脉冲。
