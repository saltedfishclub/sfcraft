# 贡献指南

欢迎！

## 贡献流程
首先，你需要新建一个有关修改内容的分支（如果你没有本仓库的修改权限，请自行 Fork），然后在里面提交代码，最后你就可以发起 Pull Request 并且等待我们审查你的代码了。

在代码审查通过后，你的代码就会被合并到 `master` 分支，最后在每天的凌晨五点 （UTC TIME）在服务器内生效（不出意外的话）

## 项目结构
SFCraft 内部使用 [guice](https://github.com/google/guice) 提供依赖注入。功能模块均为 `ServerModule` 的子类，位于 `io.ib67.sfcraft.module` 包下，请根据功能内容合理安排类的位置。

> [!IMPORTANT]     
> 在编写完 ServerModule 之后，必须在 `io.ib67.sfcraft.SFCraftInitializer#registerFeatures` 插入注册代码，否则你的模块将不会被启用。

此外，SFCraft 大量使用 Mixin 补全 Fabric API 没有涵盖到的地方，例如 `io.ib67.sfcraft.callback.SFCallbacks.PLAYER_FLYING` 等回调。所有由 SFCraft 提供的回调都位于 `io.ib67.sfcraft.callback.SFCallback` 内，当您需要扩充 API 内容时请创建对应的回调，尽可能不要直接从 Mixin 调用到具体方法。

正确例子:  
https://github.com/saltedfishclub/sfcraft/blob/7c3abc0ffdc9de495df02f34b2a2bdcb9470fce1/src/main/java/io/ib67/sfcraft/callback/SFCallbacks.java#L36-L37

使用例：  
https://github.com/saltedfishclub/sfcraft/blob/7c3abc0ffdc9de495df02f34b2a2bdcb9470fce1/src/main/java/io/ib67/sfcraft/module/ElytraSpeedMeterModule.java#L41-L45

## 注意事项

由于 SFCraft 是完全的服务端 Mod, 因此写代码时请假设总是在服务端上运行。

#### MinecraftServer

在 `onInitialize` 阶段你尚不能访问 `MinecraftServer` 实例，但是你需要在这个阶段注册命令或者访问注册表或者做别的什么东西。

如果你需要访问 MinecraftServer, 这么写：
```java
@Inject
private MinecraftServerSupplier serverSupplier;
```

在 `onEnable` 时，服务端已经加载完毕，此时你可通过 `serverSupplier#get()` 得到 MinecraftServer.

#### 从 Injector 中获取对象

在 Mixin 代码中你可能需要拿一些在 Guice 内的对象，此时可以通过 `SFCraft.getInjector().getInstance(TYPE)` 得到对应的对象。  
但是注意：如果你的 Mixin 代码是高频操作，请适当地做缓存

#### 使用自定义实体 API

参考 `SFEntityType#init`  
自定义实体的写法大体与正常 mod 开发无异。

#### 使用 Room API

文档施工中
请参考 `CreativeSpace`  
原理: [一种使用 /transfer 的轻量化子服方案](https://blog.0w0.ing/2024/07/17/multiserver-based-on-one-utilizing-transfer/)