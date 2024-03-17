package com.tac.guns.item;

import com.tac.guns.GunMod;
import com.tac.guns.init.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class AmmoBoxItem extends Item implements DyeableLeatherItem {
    public static final ResourceLocation PROPERTY_NAME = new ResourceLocation(GunMod.MOD_ID, "ammo_statue");

    public AmmoBoxItem() {
        super(new Properties().stacksTo(1).tab(ModItems.OTHER_TAB));
    }

    @OnlyIn(Dist.CLIENT)
    public static int getColor(ItemStack stack, int tintIndex) {
        return tintIndex > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack);
    }

    @OnlyIn(Dist.CLIENT)
    public static float getStatue(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return 1;
    }
}
