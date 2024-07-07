package io.ib67.sfcraft;

import io.ib67.sfcraft.access.AccessController;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public final class Commands {
    @RequiredArgsConstructor
    static class AccessControl {
        private final AccessController controller;

        public void grantAccess(ServerCommandSource sender, String name, @Nullable String ip) {
            if (!sender.hasPermissionLevel(2)) {
                sender.sendMessage(Text.of("Permission denied."));
                return;
            }
            if (!StringHelper.isValidPlayerName(name)) {
                sender.sendMessage(Text.of("Invalid player name."));
                return;
            }
            if (ip == null) {
                sender.sendMessage(Text.of("You're granting ANY access to player " + name + "!"));
                controller.grantAccess(name, null);
            } else {
                controller.grantAccess(name, ip);
            }
            sender.sendMessage(Text.of(name + ": Access granted."));
        }

        public void revokeAccess(ServerCommandSource sender, String name) {
            controller.revokeAccess(name);
            sender.sendMessage(Text.of(name + ": Permission revoked."));
        }

        public void listAccesses(ServerCommandSource sender) {
            controller.getEntries().forEach(it -> sender.sendMessage(Text.of(it.name() + ": " + (it.condition() == null ? "ANY" : it.condition()))));
        }
    }
}
