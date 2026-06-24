package com.tacz.guns.inventory.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AmmoStorageTooltip implements TooltipComponent {
    private final List<Entry> entries;
    private final int hidden;

    public AmmoStorageTooltip(List<Entry> entries, int hidden) {
        this.entries = entries;
        this.hidden = hidden;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getHidden() {
        return hidden;
    }

    public record Entry(ItemStack ammo, int count) {
        public Component getAmmoName() {
            return ammo.getHoverName();
        }
    }
}
