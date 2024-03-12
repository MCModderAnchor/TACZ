package com.tac.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IGun;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import com.tac.guns.resource.DefaultAssets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class AttachmentSkinTestCommand {
    private static final String SKIN_TEST_NAME = "askin";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(SKIN_TEST_NAME);
        reload.executes(AttachmentSkinTestCommand::applySkin);
        return reload;
    }

    private static int applySkin(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack itemStack = player.getMainHandItem();
            if(itemStack.getItem() instanceof IGun iGun){
                iGun.setAttachment(itemStack, AttachmentType.SCOPE, AttachmentItemBuilder.create().setSkinId(DefaultAssets.DEFAULT_ATTACHMENT_SKIN_ID).build());
            }
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        context.getSource().sendSuccess(new TextComponent("Attachment apply complete"), true);
        return Command.SINGLE_SUCCESS;
    }
}
