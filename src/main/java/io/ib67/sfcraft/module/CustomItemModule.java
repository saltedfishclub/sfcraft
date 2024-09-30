package io.ib67.sfcraft.module;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.item.SFItem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CustomItemModule extends ServerModule {
    @Override
    public void onInitialize() {
        SFItemRegistry.init();
        CommandRegistrationCallback.EVENT
                .register(this::registerCommand);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("sfgive")
                        .then(
                                CommandManager.argument("item", StringArgumentType.string())
                                        .executes(this::performGive)
                        )
        );
    }

    private static ItemStack getItemByName(Item item) throws CommandSyntaxException {
        var stack = new ItemStack(((SFItem)item).getMappedItem());
        var cmp = new NbtCompound();
        cmp.putString("sf_type",item.getRegistryEntry().getIdAsString());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(cmp));
        return stack;
    }


    private int performGive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            var itemName = StringArgumentType.getString(context, "item");
            if (!context.getSource().isExecutedByPlayer()) {
                context.getSource().sendMessage(Text.of("Can't be executed by non-players"));
                return 0;
            }
            var player = context.getSource().getPlayer();
            player.giveItemStack(getItemByName(SFItems.AERO_BACKPACK));
        }catch(Throwable t){
            t.printStackTrace();
        }
        return 1;
    }
}