package io.ib67.sfcraft.module;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class AFKModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_IDLE.register(this::onAFK);
    }

    private void onAFK(ServerPlayerEntity player, boolean afk) {
        if(afk){
            player.server.getPlayerManager().broadcast(
                    Text.literal(" * " + player.getName().getLiteralString() + " 正在挂机.").withColor(Colors.LIGHT_GRAY),
                    false
            );
        }else{
            player.server.getPlayerManager().broadcast(
                    Text.literal(" * " + player.getName().getLiteralString() + " 回来了.").withColor(Colors.LIGHT_GRAY),
                    false
            );
        }
    }
}
