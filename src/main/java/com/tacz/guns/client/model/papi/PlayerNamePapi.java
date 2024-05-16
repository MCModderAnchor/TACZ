package com.tacz.guns.client.model.papi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class PlayerNamePapi implements Function<ItemStack, String> {
    public static final String NAME = "player_name";

    @Override
    public String apply(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getName().getString();
        }
        return "";
    }
}
