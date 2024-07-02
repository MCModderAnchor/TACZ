package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DebugCommand {
    public static boolean DEBUG = false;
    private static final String DEBUG_NAME = "debug";
    private static final String ENABLE = "enable";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal(DEBUG_NAME);
        RequiredArgumentBuilder<CommandSourceStack, Boolean> enable = Commands.argument(ENABLE, BoolArgumentType.bool());
        debugCommand.then(enable.executes(DebugCommand::setValue));
        return debugCommand;
    }

    private static int setValue(CommandContext<CommandSourceStack> context) {
        DEBUG = BoolArgumentType.getBool(context, ENABLE);
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            if (DEBUG) {
                serverPlayer.sendSystemMessage(Component.literal("TacZ Debug Mode is Turn On"));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("TacZ Debug Mode is Turn Off"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
