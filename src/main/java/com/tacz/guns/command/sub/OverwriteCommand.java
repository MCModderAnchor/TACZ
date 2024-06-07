package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OverwriteCommand {
    private static final String OVERWRITE_NAME = "overwrite";
    private static final String ENABLE = "enable";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(OVERWRITE_NAME);
        RequiredArgumentBuilder<CommandSourceStack, Boolean> enable = Commands.argument(ENABLE, BoolArgumentType.bool());
        reload.then(enable.executes(OverwriteCommand::setOverwrite));
        return reload;
    }

    private static int setOverwrite(CommandContext<CommandSourceStack> context) {
        boolean enable = BoolArgumentType.getBool(context, ENABLE);
        OtherConfig.DEFAULT_PACK_DEBUG.set(!enable);
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            if (OtherConfig.DEFAULT_PACK_DEBUG.get()) {
                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_off"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("commands.tacz.reload.overwrite_on"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
