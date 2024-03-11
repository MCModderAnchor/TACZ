package com.tac.guns.item.builder;

import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.item.IGun;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.nbt.GunItemData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class GunItemBuilder {
    private int count = 1;
    private int ammoCount = 0;
    private ResourceLocation gunId = GunItemData.DEFAULT;
    private FireMode fireMode = FireMode.UNKNOWN;

    private GunItemBuilder() {
    }

    public static GunItemBuilder create() {
        return new GunItemBuilder();
    }

    public GunItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public GunItemBuilder setAmmoCount(int count) {
        this.ammoCount = Math.max(count, 0);
        return this;
    }

    public GunItemBuilder setId(ResourceLocation id) {
        this.gunId = id;
        return this;
    }

    public GunItemBuilder setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
        return this;
    }

    public ItemStack build() {
        ItemStack gun = new ItemStack(ModItems.GUN.get(), this.count);
        if (gun.getItem() instanceof IGun iGun) {
            iGun.setGunId(gun, this.gunId);
            iGun.setFireMode(gun, this.fireMode);
            iGun.setCurrentAmmoCount(gun, this.ammoCount);
        }
        return gun;
    }
}
