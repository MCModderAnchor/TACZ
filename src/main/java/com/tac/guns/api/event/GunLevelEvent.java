package com.tac.guns.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class GunLevelEvent extends Event {
    private final Player player;
    private final ItemStack gun;
    private final int currentLevel;

    public GunLevelEvent(Player player, ItemStack gun, int currentLevel) {
        this.player = player;
        this.gun = gun;
        this.currentLevel = currentLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getGun() {
        return gun;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }
}
