# 贡献指南

如果你不会写代码，可以在 issues 分享你的想法，只要符合这几个条件：

1. SFCraft 是原版生存服，新特性设想（尽量）不破坏原版平衡性，不干涉原版机制。（典型例子: tpa，死亡不掉落）  
   如果死亡都是一件不足挂齿的事情，那么死亡的意义何在呢？如果我们可以到处传送，那么交通工程的意义何在呢？
2. 新特性对于大多数人有价值。  
   否则没有理由开发它。

也可以在讨论群内直接提出。但是最好还是发一个 feature request 这样方便开发者们管理请求。  

如果你不仅有想法，还会开发（Java），那么请往下看
## 开始之前

首先，光会一些基本的 Java 语法可能不足以支撑你开发新特性。在这种情况下，不建议勉强。  
其次，开发 Mod 你必须要有科学上网环境，否则配置工程时可能会遇到很多问题，以至于根本没法开始开发工作。

SFCraft 是一个纯粹的服务端模组，这意味着**我们不能添加原版游戏没有的物品/方块/生物**，因此你可能不太用得上 Fabric API 的内容。然而，了解一些基本的模组开发常识也是有帮助的

[FabricMC Wiki 官方教程（全英）](https://fabricmc.net/wiki/tutorial:start)

最后，请将这篇文章看完。

## 项目结构

SFCraft 是一个 Fabric Mod，他的所有内容都是放在一起的，下列是一些关键的类：
 
 - `io.ib67.sfcraft.config.SFConfig` 这是 Mod 的配置文件。  
   在此类定义的字段将会自动作为配置文件的一部分生成。你可以通过调用 `SFCraft.getInstance().getConfig()` 得到配置对象。
 - `io.ib67.sfcraft.Listener` 是事件监听器。大多数回调/事件的处理代码放在这里。
 - `io.ib67.sfcraft.Commands` 是处理命令逻辑的地方，而命令在 `SFCraft#registerCommands` 方法注册。
 - `io.ib67.sfcraft.SFConsts` 存放一些频繁使用的常量，例如权限。
 - `io.ib67.sfcraft.mixin.server` 下是一些 `Mixin` 类，接下来会讲。

最后，`io.ib67.sfcraft.SFCraft` 是整个 Mod 的入口，也是核心部分。你可以从 `SFCraft.getInstance()` 返回的对象得到 Mod 里的大多数组件。

## 注册事件
Fabric API 有事件的情况下可以直接效仿着在 SFCraft `onInitialize` 时注册:

```java
        ServerPlayConnectionEvents.JOIN.register(listener::onPlayerJoin);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
```

通常你应该把复杂的逻辑放到 `Listener` 里。这里的 `registerCommands` 和 `onServerStarted` 是个特例，因为他们并没有太多要做的事。  

然而，没有的情况下则需要你自己 Mixin。SFCraft 的运行环境 100% 为服务端，请完全按照服务端 Mod 的写法来写。  
写 Mixin 是为了将事件转发到 Listener：
```java
@Mixin(ServerPlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        SFCraft.getInstance().getListener().onPlayerDeath((ServerPlayerEntity) (Object) this, damageSource);
    }

    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    public void onSleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        var result = SFCraft.getInstance().getListener().onPlayerSleep((ServerPlayerEntity) (Object) this, pos);
        if(result.left().isPresent()){
            cir.setReturnValue(result);
        }
    }
}
```
除了针对性特别强或者必要，不要把太多代码放在 Mixin 里面。

## 指令 & 权限

SFCraft 有自己的一套权限系统（可能会被换掉）

### 命令

SFCraft 的所有指令在 `SFCraft#registerCommands` 内注册，使用 Brigadier.  
然而，命令的逻辑需要移动到 `Commands` 以防代码囤积在 `SFCraft` 内。

### 权限

SFCraft 使用 `Permission` 类实现权限。所有在代码中使用的权限（除了 `special`）都要堆放在 `SFConsts` 常量类下：

```java
@UtilityClass
public class SFConsts {
    public static final String SPECIAL_SUDO = "sfcraft.special.state.sudo";
    public static final Permission<PlayerEntity> USE_AT = ofSFCPermission("chat.at", true);
    // .....
}
```

具体用法可以看 `SFConsts#ofSFCPermission` 的参数命名，以及 `Permission` 类本身，在此不过多赘述。要注意的是，`Permission` 局限在于他只能处理有玩家实体的情况。  
虽然这足够应付大多数情况，然而有的命令不需要玩家存在也可以使用（比如 `addwl`），这种情况下，注册命令时需要使用如下技巧：

```java
LiteralArgumentBuilder.<ServerCommandSource>literal("addwl")
    .requires(it -> it.hasPermissionLevel(2) || SFConsts.COMMAND_ADDWL.hasPermission(it.getPlayer()))
```

以此保持命令方块和控制台兼容性，注意不要写反。

## 随机事件
随机事件在 `SFRandomEventRegistry` 的构造方法内注册。其本身是 `RandomEvent` 的子类。

```java
public class SFRandomEventRegistry extends SimpleRandomEventRegistry {
    public SFRandomEventRegistry() {
        registerEvent(LongNightEvent::new, World.OVERWORLD
                , world -> !LongNightEvent.isRunning()
                        && world.getTimeOfDay() % 24000 == 18000
                        && world.getRandom().nextBetween(0,100) < 5); // At midnight
        registerEvent(DawnAfterLongNightEvent::new, World.OVERWORLD
                , world -> world.getTimeOfDay() % 22200 == 0); // At dawn
    }
}
```

由于从 "REG" 查询事件是否发生性能较慢，因此建议写随机事件的时候在 `start` 方法内标记状态，如上例中的 `LongNightEvent.isRunning()`

下面是一个非常简单的随机事件例子：
```java
@RequiredArgsConstructor
public class DawnAfterLongNightEvent extends RandomEvent {
    private final World world;

    @Override
    public int start() {
        if (LongNightEvent.justExpirencedLongNight) {
            LongNightEvent.justExpirencedLongNight = false;
            world.getServer().getPlayerManager().broadcast(
                    Text.literal("\"月亮\"离开了。")
                            .withColor(new Color(235, 91, 0).getRGB())
                            .append(Text.literal("（永夜事件结束）").withColor(Colors.LIGHT_GRAY))
                    , false);
        }
        return -1;
    }

    @Override
    public void onUpdate(int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void end() {
        throw new UnsupportedOperationException();
    }
}
```

当注册时的 Predicate 通过后他的 `start` 就会被运行，提供事件持续的时长（在本例中，`-1` 表示立即停止）。随后每一 World tick 它的 `onUpdate` 都会被执行。  
最后会被调用 `end` 结束事件。由于永夜事件结束是在 tick=18000 时，天还没亮，因此特地写了一个 `DawnAfterLongNightEvent` 实现延迟到黎明发消息。

## 聊天修改

类似 `.xyz`, `@xx` 的功能都位于 `message.feature` 包下，且在 `SFMessageDecorator` 的构造方法中被注册。  
它们实际上是 Minecraft 提供的 `MessageDecorator` 接口的实现。

通常，一条消息进入服务器后会轮流经过许多 decorator 的处理。因此为了避免 siblings 嵌套问题，`SimpleMessageDecorator` (也就是 `SFMessageDecorator` 的超类) 会自动将 decorator 返回的 Text 展开——
具体的说，将返回的结果 copy,去除 siblings，然后将 siblings 逐个加入一个缓存队列中，下一个 Decorator 将会逐个处理队列中的 `Text`，且每次处理完后都会清理掉原本的缓存队列。因此，在没有特殊需求的情况下，请总是返回你 Decorator 不关注的信息。

一个简单的例子：
```java
    @Override
    public Text decorate(@Nullable ServerPlayerEntity sender, Text message) {
    if (sender != null) {
        if (!SFConsts.USE_AT.hasPermission(sender)) {
            return message;
        }
    }
    var text = message.getLiteralString();
    if (text == null) return message;
}
```
## 配置文件

它比较像摆设。  
与一般的 Mod 不同，SFCraft 没有 Client-Side Translatable Key 可用，因此一些不常修改的信息可以直接硬编码字符。  
在未来可能会加入多语言支持，如果有外国人的话。  

## 其他

一些工具函数可以堆放在 `Helper` 类。

提交的新功能负责人要顺便去 wiki 写一下章节，因为新功能以后还会越来越多