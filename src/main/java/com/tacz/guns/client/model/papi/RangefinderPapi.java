package com.tacz.guns.client.model.papi;

import com.tacz.guns.client.resource.pojo.display.gun.RangefinderManager;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class RangefinderPapi implements Function<ItemStack, String> {
    public static final String NAME = "rangefinder";

    @Override
    public String apply(ItemStack stack) {
        return RangefinderManager.getText(stack);
    }
}
