package com.tac.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class AttachmentTestCommand {
    private static final String ATTACHMENT_TEST_NAME = "attachment";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(ATTACHMENT_TEST_NAME);
        reload.executes(AttachmentTestCommand::applyAttachment);
        return reload;
    }

    private static int applyAttachment(CommandContext<CommandSourceStack> context) {
        try {
            AttachmentTest.testAttachment(context.getSource().getPlayerOrException());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        context.getSource().sendSuccess(new TextComponent("Attachment apply complete"), true);
        return Command.SINGLE_SUCCESS;
    }
}
