package com.tacz.guns.command.sub;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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

public class DummyAmmoCommand {
    private static final String DUMMY_NAME = "dummy";
    private static final String ENTITY = "target";
    private static final String AMOUNT = "dummyAmount";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> dummy = Commands.literal(DUMMY_NAME);
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> entities = Commands.argument(ENTITY, EntityArgument.entities());
        RequiredArgumentBuilder<CommandSourceStack, Integer> amount = Commands.argument(AMOUNT, IntegerArgumentType.integer(0));
        dummy.then(entities.then(amount.executes(DummyAmmoCommand::setDummy)));
        return dummy;
    }

    private static int setDummy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var entities = EntityArgument.getEntities(context, ENTITY);
        int cnt = 0;
        int amount = IntegerArgumentType.getInteger(context, AMOUNT);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                ItemStack stack = living.getMainHandItem();
                if (stack.getItem() instanceof IGun iGun) {
                    iGun.setDummyAmmoAmount(stack, amount);
                    cnt++;
                }
            }
        }
        return cnt;
    }
}
