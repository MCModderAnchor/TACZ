package com.tacz.guns.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tacz.guns.command.sub.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RootCommand {
    private static final String ROOT_NAME = "tacz";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires((source -> source.hasPermission(2)));
        root.then(AttachmentLockCommand.get());
        root.then(DebugCommand.get());
        root.then(DummyAmmoCommand.get());
        root.then(OverwriteCommand.get());
        root.then(ReloadCommand.get());
        root.then(HideTooltipPartCommand.get());
        root.then(ConvertCommand.get());
        dispatcher.register(root);
    }
}
