package com.tac.guns.client.render.item;

import com.tac.guns.GunMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Dist.CLIENT)
public class OverrideModelManager {
    private static final Map<Item, IOverrideModel> MODEL_MAP = new HashMap<>();

    public static void register(Item item, IOverrideModel model) {
        if (MODEL_MAP.putIfAbsent(item, model) == null) {
            MinecraftForge.EVENT_BUS.register(model);
        }
    }

    public static boolean hasModel(ItemStack stack) {
        return MODEL_MAP.containsKey(stack.getItem());
    }

    @Nullable
    public static IOverrideModel getModel(ItemStack stack) {
        return MODEL_MAP.get(stack.getItem());
    }

    @Nullable
    public static IOverrideModel getModel(Item item) {
        return MODEL_MAP.get(item);
    }
}
