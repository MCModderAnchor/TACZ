package com.tacz.guns.resource;

import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class DedicatedServerReloadManager {
    public static void loadGunPack() {
        CommonGunPackLoader.init();
        CommonGunPackLoader.reloadAsset();
        CommonGunPackLoader.reloadIndex();
        CommonGunPackLoader.reloadRecipes();
    }

    public static void reloadFromCommand(CommandContext<CommandSourceStack> context) {
        StopWatch watch = StopWatch.createStarted();
        {
            loadGunPack();
            CommonGunPackNetwork.syncClient(context.getSource().getLevel());
        }
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendMessage(new TextComponent("Model loading time: " + time + " ms"), Util.NIL_UUID);
        }
        GunMod.LOGGER.info("Model loading time: {} ms", time);
    }
}
