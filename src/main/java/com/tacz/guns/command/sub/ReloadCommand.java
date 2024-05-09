package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.client.resource.ClientReloadManager;
import com.tacz.guns.resource.DedicatedServerReloadManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ReloadCommand {
    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD_NAME);
        reload.executes(ReloadCommand::reloadAllPack);
        return reload;
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientReloadManager::reloadAllPack);
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> DedicatedServerReloadManager.reloadFromCommand(context));
        context.getSource().sendSuccess(new TranslatableComponent("commands.tacz.reload.success"), true);
        return Command.SINGLE_SUCCESS;
    }
}
