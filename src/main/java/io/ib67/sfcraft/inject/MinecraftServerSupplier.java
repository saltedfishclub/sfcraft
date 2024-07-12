package io.ib67.sfcraft.inject;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;

public class MinecraftServerSupplier implements Supplier<MinecraftServer> {
    @Override
    @SuppressWarnings("removal")
    public MinecraftServer get() {
        return SFCraft.server;
    }
}
