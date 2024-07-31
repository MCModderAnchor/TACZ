package com.tacz.guns.resource;

import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import net.minecraft.commands.CommandSourceStack;

public class DedicatedServerReloadManager {
    public static void loadGunPack() {
        // 版本检查重置
        VersionChecker.clearCache();
        CommonGunPackLoader.init();
        CommonGunPackLoader.reloadAsset();
        CommonGunPackLoader.reloadIndex();
        CommonGunPackLoader.reloadRecipes();
    }

    public static void reloadFromCommand(CommandContext<CommandSourceStack> context) {
        loadGunPack();
        CommonGunPackNetwork.syncClient(context.getSource().getLevel().getServer());
    }
}
