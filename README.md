# SFCraft

SFCraft is a fabric mod tailored for the Minecraft server of the same name, including many fixes, features and other functionality.

**It is not recommended to use this mod on your server without proper configuration!!**

This project is open source to allow players to contribute. [Contribution GUIDE](./CONTRIBUTING.md)

You may also need our source if you're implementing some of the features listed below.

# Features

1. Hybrid offline & online authentication: Check [ServerLoginNetworkHandlerMixin](src/main/java/io/ib67/sfcraft/mixin/server/ServerLoginNetworkHandlerMixin.java)
2. Worldwide regions, which divide the server into multiple "rooms" with data strictly isolated: See [this blog](https://blog.0w0.ing/2024/07/17/multiserver-based-on-one-utilizing-transfer/)
  The implementation is mainly in [subserver](src/main/java/io/ib67/sfcraft/mixin/server/subserver/)
3. Server-side entities, the custom entities, but tied to the vanilla module: Check and look for references to [SFEntityType] (src/main/java/io/ib67/sfcraft/SFEntityType.java)
4. .. and more.

Besides the above, there are also many tiny features in the [module](src/main/java/io/ib67/sfcraft/module/) package.

# About our server

SFCraft is a whitelisted Minecraft server. To join us, contact [@ib67_pm_bot](https://t.me/ib67_pm_bot) in telegram (We chat in Chinese.)
