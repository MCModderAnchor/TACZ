package com.tacz.guns.api.item.builder;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class AmmoItemBuilder {
    private int count = 1;
    private ResourceLocation ammoId = DefaultAssets.DEFAULT_AMMO_ID;

    private AmmoItemBuilder() {
    }

    public static AmmoItemBuilder create() {
        return new AmmoItemBuilder();
    }

    public AmmoItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public AmmoItemBuilder setId(ResourceLocation id) {
        this.ammoId = id;
        return this;
    }

    public ItemStack build() {
        ItemStack ammo = new ItemStack(ModItems.AMMO.get(), this.count);
        if (ammo.getItem() instanceof IAmmo iAmmo) {
            iAmmo.setAmmoId(ammo, this.ammoId);
        }
        return ammo;
    }
}
