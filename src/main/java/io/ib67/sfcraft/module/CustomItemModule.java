package io.ib67.sfcraft.module;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.ServerModule;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomItemModule extends ServerModule {
    @Override
    public void onInitialize() {
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

    private static ItemStack getItemByName(RegistryEntry<Item> registryEntry) throws CommandSyntaxException {
        var isa = new ItemStackArgument(registryEntry, ComponentChanges.EMPTY);
        return isa.createStack(1, false);
    }


    private int performGive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var itemName = StringArgumentType.getString(context, "item");
        if (!context.getSource().isExecutedByPlayer()) {
            context.getSource().sendMessage(Text.of("Can't be executed by non-players"));
            return 0;
        }
        var player = context.getSource().getPlayer();
            player.giveItemStack(getItemByName(SFItems.AERO_BACKPACK.getRegistryEntry()));
        return 1;
    }
}
