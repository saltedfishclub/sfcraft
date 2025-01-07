package io.ib67.sfcraft.module.supervisor;

import io.ib67.sfcraft.ServerModule;
import io.javalin.Javalin;

public abstract class WebHandler extends ServerModule {
    protected abstract void register(Javalin javalin);
}
