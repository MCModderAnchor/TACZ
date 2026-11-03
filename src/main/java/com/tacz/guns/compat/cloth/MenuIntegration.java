package com.tacz.guns.compat.cloth;

import com.tacz.guns.compat.cloth.client.*;
import com.tacz.guns.compat.cloth.common.AmmoClothConfig;
import com.tacz.guns.compat.cloth.common.GunClothConfig;
import com.tacz.guns.compat.cloth.common.OtherClothConfig;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.PreLoadConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import javax.annotation.Nullable;

public class MenuIntegration {
    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.literal("Timeless and Classics Guns"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        root.setSavingRunnable(() -> {
            if (ClientConfig.CLIENT_CONFIG_SPEC != null) {
                ClientConfig.CLIENT_CONFIG_SPEC.save();
            }
            if (CommonConfig.COMMON_CONFIG_SPEC != null) {
                CommonConfig.COMMON_CONFIG_SPEC.save();
            }
            PreLoadConfig.save();
        });
        ConfigEntryBuilder entryBuilder = root.entryBuilder();

        KeyClothConfig.init(root, entryBuilder);
        RenderClothConfig.init(root, entryBuilder);
        ResourceClothConfig.init(root, entryBuilder);
        SoundClothConfig.init(root, entryBuilder);
        ZoomClothConfig.init(root, entryBuilder);

        GunClothConfig.init(root, entryBuilder);
        AmmoClothConfig.init(root, entryBuilder);
        OtherClothConfig.init(root, entryBuilder);

        return root;
    }

    public static void registerModsPage() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
                new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> getConfigScreen(parent)));
    }

    public static Screen getConfigScreen(@Nullable Screen parent) {
        return MenuIntegration.getConfigBuilder().setParentScreen(parent).build();
    }
}
