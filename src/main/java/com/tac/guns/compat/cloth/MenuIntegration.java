package com.tac.guns.compat.cloth;

import com.tac.guns.compat.cloth.client.KeyClothConfig;
import com.tac.guns.compat.cloth.client.RenderClothConfig;
import com.tac.guns.compat.cloth.common.AmmoClothConfig;
import com.tac.guns.compat.cloth.common.OtherClothConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

public class MenuIntegration {
    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(new TextComponent("Timeless and Classics Guns"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();

        KeyClothConfig.init(root, entryBuilder);
        RenderClothConfig.init(root, entryBuilder);
        AmmoClothConfig.init(root, entryBuilder);
        OtherClothConfig.init(root, entryBuilder);

        return root;
    }

    public static void registerModsPage() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () ->
                new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> getConfigBuilder().setParentScreen(parent).build()));
    }
}
