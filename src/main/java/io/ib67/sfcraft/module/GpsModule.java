package io.ib67.sfcraft.module;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.ib67.sfcraft.SFEntityType;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.entity.SFGuiderEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class GpsModule extends ServerModule {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            dispatcher.register(CommandManager.literal("gps")
                    .then(CommandManager.argument("target", BlockPosArgumentType.blockPos())
                            .executes(this::onGps)));
        });
    }

    private int onGps(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrThrow();
        var target = BlockPosArgumentType.getBlockPos(context, "target");
        var level = player.getServerWorld();
        var guide = new SFGuiderEntity(SFEntityType.GUIDER, level, player, target);
        guide.setPosition(player.getPos());
        level.spawnEntity(guide);
        return 1;
    }
}
