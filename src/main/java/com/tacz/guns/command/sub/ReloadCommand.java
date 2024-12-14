package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class ReloadCommand {
    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD_NAME);
        reload.executes(ReloadCommand::reloadAllPack);
        return reload;
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        StopWatch watch = StopWatch.createStarted();
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ReloadCommand::reloadClient);
            DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> CommonAssetsManager::reloadAllPack);
        }
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
//        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
//            serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.success", time));
//            if (OtherConfig.DEFAULT_PACK_DEBUG.get()) {
//                serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.overwrite_off"));
//                serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.overwrite_command.off"));
//            } else {
//                serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.overwrite_on"));
//                serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.overwrite_command.on"));
//                serverPlayer.sendMessage(new TranslatableComponent("commands.tacz.reload.backup"));
//            }
//        }
//        GunMod.LOGGER.info("Model loading time: {} ms", time);
        context.getSource().sendMessage(new TranslatableComponent("commands.tacz.reload.success", time), Util.NIL_UUID);
        return Command.SINGLE_SUCCESS;
    }

    public static void reloadClient() {
        ClientAssetsManager.reloadAllPack();
    }
}
