package com.tacz.guns.command.sub;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.item.IGun;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class AttachmentLockCommand {
    private static final String ATTACHMENT_LOCK_NAME = "attachment_lock";
    private static final String ENTITY = "target";
    private static final String GUN_ATTACHMENT_LOCK = "AttachmentLock";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> attachmentLock = Commands.literal(ATTACHMENT_LOCK_NAME);
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> entities = Commands.argument(ENTITY, EntityArgument.entities());
        RequiredArgumentBuilder<CommandSourceStack, Boolean> locked = Commands.argument(GUN_ATTACHMENT_LOCK, BoolArgumentType.bool());
        attachmentLock.then(entities.then(locked.executes(AttachmentLockCommand::setAttachmentLock)));
        return attachmentLock;
    }

    private static int setAttachmentLock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var entities = EntityArgument.getEntities(context, ENTITY);
        int cnt = 0;
        boolean locked = BoolArgumentType.getBool(context, GUN_ATTACHMENT_LOCK);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                ItemStack stack = living.getMainHandItem();
                if (stack.getItem() instanceof IGun iGun) {
                    iGun.setAttachmentLock(stack, locked);
                    cnt++;
                }
            }
        }
        return cnt;
    }
}
