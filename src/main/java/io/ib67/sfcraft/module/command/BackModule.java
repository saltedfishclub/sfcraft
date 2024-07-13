package io.ib67.sfcraft.module.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.ib67.sfcraft.util.Helper;
import io.ib67.sfcraft.util.SFConsts;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class BackModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_DEATH.register(this::onPlayerDeath);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("back")
                .requires(it -> this.isEnabled())
                .requires(it -> it.getPlayer() != null)
                .requires(it -> it.getPlayer().getLastDeathPos().isPresent())
                .requires(it -> SFConsts.COMMAND_BACK.hasPermission(it.getPlayer()))
                .executes(this::onBack));
    }

    public int onBack(CommandContext<ServerCommandSource> it) {
        var player = it.getSource().getPlayer();
        var pos = player.getLastDeathPos().get();
        var wld = player.server.getWorld(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return 0;
        var nearby = wld.getClosestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 20, true);
        if (nearby != null) {
            _pos = nearby.getBlockPos();
            Helper.teleportSafely(player, wld, _pos.getX(), _pos.getY(), _pos.getZ());
        } else {
            if (SFConsts.UNLIMITED_COMMAND_BACK.hasPermission(player)) {
                Helper.teleportSafely(player, wld, _pos.getX(), _pos.getY(), _pos.getZ());
                return 0;
            }
            player.sendMessage(Text.of("周围没有玩家。").copy().withColor(Colors.RED));
        }
        return 0;
    }

    public void onPlayerDeath(PlayerEntity player, DamageSource damageSource) {
        if (this.isEnabled() && Helper.canBack((ServerPlayerEntity) player)) {
            player.sendMessage(Text.of("Tip: 死亡地点附近有玩家，可以使用 /back 传送到他们那里。（即使在对方也死亡的状态下）"));
        }
    }
}
