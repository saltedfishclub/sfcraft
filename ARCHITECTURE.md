# SFCraft 架构

Fabric **纯服务端** mod(`fabric.mod.json` 中 `environment: "server"`),Minecraft **26.2**,Java 25,**官方 Mojang 映射**。所有自定义内容通过 [Polymer](https://polymer.pb4.eu) 伪装成原版内容;玩家使用原版客户端 + 服务器发布的材质包即可游玩。

## 分层

代码全部位于 `io.ib67.sfcraft`,按职责分为三层:

```
io.ib67.sfcraft
├── (根)                  SFCraft / SFCraftInitializer / ServerModule / Lifecycle
├── init/                 GuiceModInitializer(Fabric 入口基类)、PreInitHandler
├── inject/               DI 注解与供给器(@ConfigRoot、@ConfigResource、MinecraftServerSupplier)
├── config/               SFConfig(config.json)、GameConfig + GameConfigService(gameplay.json)
├── callback/             SFCallbacks(玩家事件)、SFConfigReload(配置重载)
├── registry/             注册表接口(RoomRegistry、RandomEventRegistry、CauldronRecipeRegistry…)
│   └── <name>/           对应实现(SimpleRoomRegistry、SimpleCauldronRecipeRegistry…)
├── module/               服务器功能模块(chat/、command/、room/、supervisor/、compat/…)
│   └── game/             ★ 游戏玩法内容,按特性分包(见下)
├── mixin/                所有 mixin(配置 sfcraft.mixins.json)
└── util/、room/、subserver/、geoip/、entity/   支撑代码
```

## 启动与生命周期

入口是 `SFCraftInitializer`(继承 `GuiceModInitializer`,同时本身是一个 Guice `AbstractModule`):

1. **ModInit**:创建 Injector(`SFCraft` + initializer 自身两个 Guice module)→ 按 `registerFeature()` 的**注册顺序**逐个实例化 `ServerModule` 并调用 `onInitialize()`。此阶段静态注册表未冻结,**所有注册表写入必须在这里完成**;但 `MinecraftServer` 尚不可用。
2. **SERVER_STARTED**:全部模块切换到 `Lifecycle.State.ENABLED`(触发 `onEnable()`),此时可通过注入的 `MinecraftServerSupplier` 拿到 server。
3. **SERVER_STOPPING**:切换到 `DISABLED`(触发 `onDisable()`)。模块状态变更抛异常则进入 `ERROR`。

DI 约定:

- 服务在 `SFCraftInitializer#registerServices` 绑定(接口 → 实现,单例);功能模块用 `registerFeature()`(自动单例 + 进入初始化列表)。
- 模块内依赖一律 `@Inject` 字段注入。
- **mixin 无法注入**,按既有先例用 `SFCraft.getInjector().getInstance(...)`(见 `EntityMixin`、`MobCommanderMixin`)。
- **方块实体/实体无法注入**,由所属模块在注册时用工厂闭包传入依赖(见 `CauldronModule`:`(pos, state) -> new AmethystCauldronBlockEntity(type, recipes, config, pos, state)`)。

## 配置体系

| 文件 | 载体 | 加载 | 热重载 |
|---|---|---|---|
| `sfcraft/config.json` | `SFConfig`(POJO 单例,直接注入) | `SFCraft#loadConfig` @Provides | 否(重启生效) |
| `sfcraft/gameplay.json` | `GameConfig`(POJO),经 `GameConfigService` 持有 | `GameExtensionModule.onInitialize()` 首次加载 | 是(`/sfcraft reload`) |
| `sfcraft/motd.txt` 等资源 | `@ConfigResource` 字符串 | 各模块 @Provides | MotdModule 监听重载 |

**重载机制**:`/sfcraft reload`(`module.command.ReloadCommandModule`,权限 LEVEL_GAMEMASTERS)只做一件事——触发 `callback.SFConfigReload` 事件。需要热重载的模块自行监听:

- `GameExtensionModule` → 重读 gameplay.json
- `TokenModule` → 重建自己注册的炼药锅配方(应用新 reactionTicks)
- `MotdModule` → 重读 motd.txt 并即时生效

监听器按注册顺序执行;`GameExtensionModule` 注册最早,保证其它监听器执行时读到的已是新配置。战利品概率在数据包加载时取样,改完还需再执行原版 `/reload`。

## 游戏内容层(module.game)

每个玩法特性一个包,包内含一个 `ServerModule`(在 `onInitialize()` 注册该特性的全部注册表内容并持有实例)及其方块/物品/实体类。注册用 `RegistryHelper` 静态工具(自动处理 `setId`、Polymer 的 `registerBlockEntity`/`registerType`)。

| 包 | 模块 | 内容 |
|---|---|---|
| `game/` | `GameExtensionModule` | gameplay.json 生命周期(**必须在其它 game 模块之前注册**——部分模块注册物品时会把配置值烘焙进组件,如反向信物耐久) |
| `game/cauldron/` | `CauldronModule` | 紫水晶炼药锅方块 + BE(吸入掉落物、按配方自动反应)+ 两个物品;配方来自 `registry.CauldronRecipeRegistry`,本模块不写死配方 |
| `game/crystal/` | `GravityCrystalModule` | 重力水晶(伪装重生锚,紫水晶碎片充能,范围内速度矢量加速) |
| `game/bomb/` | `BombModule` | 三种炸弹物品 + 共用 `BombEntity`(伪装雪球)+ 战利品注入(地牢箱、苦力怕) |
| `game/token/` | `TokenModule` | 珍珠信物/反向信物(owner 存 `custom_data`)+ 向炼药锅注册"信物+回响碎片→反向信物"配方 |
| `game/lunchbox/` | `LunchBoxModule` | 午餐盒(`food`+`consumable` 组件驱动原版进食动画,内容存 `container` 组件,GENERIC_9x1 菜单) |
| `game/beheading/` | `BeheadingModule` | 斩首附魔掉头逻辑(附魔本体数据驱动:`data/sfcraft/enchantment/beheading.json`) |
| `game/commander/` | `CommanderModule` | 「统帅」效果注册 + 光环/纽带/整队跟随行为(实例状态);刷新概率入口在 `MobCommanderMixin` |
| `game/mount/` | `MountModule` | 劫掠兽/疣猪兽驯服坐骑交互;`MountLogic` 为静态共享逻辑供 mixin 复用,`MountAccess` 为 mixin 鸭子接口 |
| `game/item/` | — | 跨特性共享的物品基类(`SimplePolymerItem`) |

跨特性协作靠注册表接口解耦:炼药锅系统只消费 `CauldronRecipeRegistry`,信物特性向其贡献配方,二者互不引用。

## Mixin 组织

`io.ib67.sfcraft.mixin`,按用途分包(`sfcraft.mixins.json` 同步维护):

| 包 | 用途 |
|---|---|
| `common/` | 基础设施注入:发布 `SFCallbacks` 事件(EntityMixin、PlayerEntityMixin)、供子 mixin 复用的 shadow(MobEntityMixin) |
| `common/bridge/` | 暴露原版内部状态的 accessor/bridge |
| `common/fixes/` | 原版行为修正(蜜蜂挤压、末影人不消失等) |
| `gameplay/` | ★ 玩法特性 mixin(MobCommanderMixin、ZombieMixin、`mount/` 骑乘覆写);保持轻薄,逻辑委托给 `module.game` 的模块或 `MountLogic` |
| `server/` | 服务端网络/世界层(登录、MOTD、睡眠、furnace ticker…) |
| `server/subserver/` | 多"房间"子服体系(数据隔离、transfer) |

约定:mixin 里尽量不直接调业务方法——要么发布 `SFCallbacks` 回调由模块监听,要么(gameplay)一行委托给模块公开方法。

## i18n

玩家可见文本一律 `Component.translatable(key)`,键值在 `assets/sfcraft/lang/{zh_cn,en_us}.json`。**不引入 server-translations 之类的服务端翻译库**——玩家会安装服务器材质包,lang 文件随包分发,由客户端解析。键名约定:`item.sfcraft.*`、`block.sfcraft.*`、`message.sfcraft.<feature>.*`、`lore.sfcraft.*`、`container.sfcraft.*`、`command.sfcraft.*`、`enchantment.sfcraft.*`。物品显示名用 `ITEM_NAME` 组件挂 translatable(Polymer 会把组件带给客户端,否则客户端显示伪装物品的原版名)。

## 数据驱动内容(resources)

- `data/sfcraft/recipe/` — 合成/熔炼配方(26.2 单数目录名)
- `data/sfcraft/loot_table/blocks/` — 方块战利品表(精准采集用 `alternatives` + `match_tool`)
- `data/sfcraft/enchantment/beheading.json` — 数据驱动附魔(描述用 translate + fallback)
- `data/sfcraft/villager_trade/` + `data/minecraft/tags/villager_trade/` — 26.2 数据驱动村民交易
- `data/minecraft/tags/` — 挖掘工具、附魔池等 tag 合并
- `assets/sfcraft/` — 物品模型/贴图/lang,随服务器材质包分发
