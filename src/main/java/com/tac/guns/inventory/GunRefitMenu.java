package com.tac.guns.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import javax.annotation.Nonnull;

public class GunRefitMenu extends AbstractContainerMenu {
    public static final MenuType<GunRefitMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> new GunRefitMenu(windowId, inv));

    public GunRefitMenu(int id, Inventory inventory) {
        super(TYPE, id);
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
