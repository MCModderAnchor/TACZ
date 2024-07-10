package com.tacz.guns.command.sub;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.item.GunTooltipPart;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HideTooltipPartCommand {
    private static final String HIDE_TOOLTIP_PART_NAME = "hide_tooltip_part";
    private static final String ENTITY = "target";
    private static final String MASK = "mask";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal(HIDE_TOOLTIP_PART_NAME);
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> entities = Commands.argument(ENTITY, EntityArgument.entities());
        RequiredArgumentBuilder<CommandSourceStack, Integer> part = Commands.argument(MASK, IntegerArgumentType.integer(0));
        base.then(entities.then(part.executes(HideTooltipPartCommand::setHide)));
        return base;
    }

    private static int setHide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var entities = EntityArgument.getEntities(context, ENTITY);
        int cnt = 0;
        int mask = IntegerArgumentType.getInteger(context, MASK);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                ItemStack stack = living.getMainHandItem();
                if (stack.getItem() instanceof IGun) {
                    GunTooltipPart.setHideFlags(stack, mask);
                    cnt++;
                }
            }
        }
        return cnt;
    }
}
