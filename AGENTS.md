# sfcraft Agent 工作指南

Fabric **纯服务端** mod,Minecraft **26.2**,Java 25,官方 Mojang 映射,自定义内容经 [Polymer](https://polymer.pb4.eu) 伪装成原版内容。整体结构见 [ARCHITECTURE.md](./ARCHITECTURE.md),先读它再动代码。

## ⚠️ 工作范围(最高优先级规则)

Agent 的主要工作范围**只有**这两个包:

- `io.ib67.sfcraft.module.game` — 游戏玩法模块
- `io.ib67.sfcraft.mixin.gameplay` — 玩法相关 mixin

**其它任何位置在修改之前必须先向用户说明并获得明确许可**,包括但不限于:

- `io.ib67.sfcraft` 其余包(基础设施、DI、callback、registry、其他 module、其他 mixin)
- `SFCraftInitializer`(即使只是给新模块加一行 `registerFeature`)
- `sfcraft.mixins.json`(即使只是加一条 gameplay mixin 条目)
- `src/main/resources/` 下的 assets/data(lang、配方、战利品表、tag…)
- 构建脚本、accesswidener、fabric.mod.json、文档

新特性通常必然涉及 `registerFeature` 注册行、mixins.json 条目、lang 键、配方 JSON 这些"例行越界"——请在给出方案时一并列出这些改动点,**打包征得一次同意**后再动手,不要做完了才说。

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

## 新增/修改玩法特性的套路

1. 在 `module.game.<feature>` 建包:一个 `ServerModule` 子类 + 该特性的方块/物品/实体类。
2. 注册表内容在模块 `onInitialize()` 里用 `RegistryHelper` 注册(此阶段注册表未冻结、`MinecraftServer` 不可用),实例存模块字段(`@Getter`)。**不要写 `public static void register()` 或静态注册表持有类。**
3. 依赖一律 `@Inject`(如 `GameConfigService`、`CauldronRecipeRegistry`);方块实体/实体拿不到注入,由模块在注册时用工厂闭包把依赖塞进构造器(参考 `CauldronModule`)。
4. mixin 放 `mixin.gameplay`,保持轻薄:逻辑写在模块的公开方法里,mixin 用 `SFCraft.getInjector().getInstance(XxxModule.class)` 一行委托(参考 `MobCommanderMixin`);高频调用路径可参考 `MountLogic` 的惰性缓存。能用 Fabric API 事件或 `SFCallbacks` 回调就不要写 mixin。
5. 需要注册模块时在 `SFCraftInitializer#registerGameFeatures` 加一行(⚠️ 越界,需许可)。**`GameExtensionModule` 必须保持在所有 game 模块之前**——它加载 gameplay.json,后续模块注册物品时会读配置。
6. 编译验证,必要时 runServer 冒烟。

## 编码约定

- **配置**:数值参数进 `GameConfig`(`io.ib67.sfcraft.config`)对应特性的嵌套类,运行期通过注入的 `GameConfigService#get()` **每次现读,不要缓存**(否则 `/sfcraft reload` 不生效)。注册时烘焙进组件的值(如耐久)天然不可热重载,属已知限制。需要在重载时重建的东西(如炼药锅配方)监听 `callback.SFConfigReload`。
- **i18n**:玩家可见文本一律 `Component.translatable`,键加进 `assets/sfcraft/lang/zh_cn.json` **和** `en_us.json`(⚠️ 越界,需许可)。**禁止**硬编码中文/英文 literal。不要引入 server-translations 依赖——玩家装服务器材质包,lang 由客户端解析。物品显示名 = `ITEM_NAME` 组件挂 translatable。
- **纯服务端**:假设代码总在服务端运行;玩家移动是客户端权威,服务端改玩家速度矢量要置 `entity.hurtMarked = true`。

## 踩过的坑

**26.2 Mojmap 重命名**(相对 1.21.x,写代码前先 javap 确认):

- `ResourceLocation` → `net.minecraft.resources.Identifier`(`Identifier.fromNamespaceAndPath`)
- EntityType 常量在 `net.minecraft.world.entity.EntityTypes`(`EntityTypes.SNOWBALL`)
- `Level.isClientSide` 字段私有化 → 用 `isClientSide()`;`Level.random` → `getRandom()`
- BE 存档:`saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`,列表用 `out.list(name, Codec)` / `in.listOrEmpty(...)`
- `BlockBehaviour.onRemove` 已删 → 覆写 `BlockEntity.preRemoveSideEffects`
- 类移包:`Zombie` → `entity.monster.zombie`,`ThrowableItemProjectile` → `entity.projectile.throwableitemprojectile`,`VillagerProfession` → `entity.npc.villager`
- `MobEffects.SLOWNESS`(不是 MOVEMENT_SLOWDOWN);`SimpleContainer` 没有 addListener 了(覆写 `setChanged()` 持久化)
- `GameProfile` 是 record(`name()`/`id()`);`sendOverlayMessage`/`sendSystemMessage` 取代 `displayClientMessage`;命令权限 `Commands.LEVEL_GAMEMASTERS.check(source.permissions())`

**Polymer(0.17.3+26.2)**:

- `PacketContext` 在 `net.fabricmc.fabric.api.networking.v1.context.PacketContext`
- BE 类型必须 `PolymerBlockUtils.registerBlockEntity`,实体类型必须 `PolymerEntityUtils.registerType`(`RegistryHelper` 已封装)
- 伪装物品尽量选**非方块、无自定义 use 行为**的原版物品:方块物品有放置鬼影;有 use 覆写的物品会吃掉客户端 consumable 预测(进食动画不显示)
- 要让客户端拿到额外组件(如 `food`/`consumable`),覆写 `getPolymerItemStack` 显式复制
- 自定义 MobEffect 必须实现 `PolymerMobEffect` 并返回原版替身(静态非同步注册表)

**加载时序**:

- 初始数据包加载**早于物品组件初始化**,自定义 Codec 里不能用 `ItemStack.CODEC`(报 "Item ... does not have components yet")——只存物品 id(`BuiltInRegistries.ITEM.holderByNameCodec()`),用时再 `new ItemStack(holder)`
- 注册必须在 `onInitialize()`(ModInit);`onEnable` 阶段注册表已冻结
- 模块按 `registerFeature` 顺序初始化;`SFConfigReload` 监听器按注册顺序执行
