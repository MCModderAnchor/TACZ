package com.tac.guns.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> new GunSmithTableMenu(windowId, inv));

    public GunSmithTableMenu(int id, Inventory inventory) {
        super(TYPE, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
