package com.tac.guns.client.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CustomTab extends CreativeModeTab {
    private final String key;
    private final Component displayName;
    private final ItemStack icon;

    public CustomTab(String key, String nameKey, ItemStack icon) {
        super(key);
        this.key = key;
        this.displayName = new TranslatableComponent(nameKey);
        this.icon = icon;
    }

    @Override
    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public ItemStack makeIcon() {
        return icon;
    }

    public String getKey() {
        return key;
    }
}
