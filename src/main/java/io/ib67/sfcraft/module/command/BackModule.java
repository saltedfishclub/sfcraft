package io.ib67.sfcraft.module.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class BackModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_DEATH.register(this::onPlayerDeath);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection registrationEnvironment) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("back")
                .requires(it -> this.isEnabled())
                .requires(it -> it.getPlayer() != null && it.getPlayer().getLastDeathLocation().isPresent())
                .requires(it -> SFConsts.COMMAND_BACK.hasPermission(it.getPlayer()))
                .executes(this::onBack));
    }

    public int onBack(CommandContext<CommandSourceStack> it) {
        var player = it.getSource().getPlayer();
        if (player == null) return 0;
        var pos = player.getLastDeathLocation().get();
        var wld = player.level().getServer().getLevel(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return 0;
        var nearby = wld.getNearestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 200, true);
        if (nearby != null) {
            _pos = nearby.blockPosition();
            Helper.teleportSafely(player, wld, _pos.getX(), _pos.getY(), _pos.getZ(),0,0);
        } else {
            if (SFConsts.UNLIMITED_COMMAND_BACK.hasPermission(player)) {
                Helper.teleportSafely(player, wld, _pos.getX(), _pos.getY(), _pos.getZ(),0,0);
                return 0;
            }
            player.sendSystemMessage(Component.nullToEmpty("周围没有玩家。").copy().withColor(CommonColors.RED));
        }
        return 0;
    }

    public void onPlayerDeath(Player player, DamageSource damageSource) {
        if (this.isEnabled() && Helper.canBack((ServerPlayer) player)) {
            player.sendSystemMessage(Component.nullToEmpty("Tip: 死亡地点附近有玩家，可以使用 /back 传送到他们那里。（即使在对方也死亡的状态下）"));
        }
    }
}
