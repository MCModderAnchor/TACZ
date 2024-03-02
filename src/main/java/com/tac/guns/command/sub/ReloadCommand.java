package com.tac.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tac.guns.client.event.ReloadResourceEvent;
import com.tac.guns.resource.CommonGunPackLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public class ReloadCommand {
    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD_NAME);
        reload.executes(ReloadCommand::reloadAllPack);
        return reload;
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ReloadResourceEvent::reloadAllPack);
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            CommonGunPackLoader.init();
            CommonGunPackLoader.reloadAsset();
            CommonGunPackLoader.reloadIndex();
        });
        context.getSource().sendSuccess(new TranslatableComponent("commands.tac.reload.success"), true);
        return Command.SINGLE_SUCCESS;
    }
}
