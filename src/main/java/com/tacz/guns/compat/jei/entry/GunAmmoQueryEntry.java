package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 枪械与其使用的弹药物品的对应关系。
 */
public class GunAmmoQueryEntry {
    private final ItemStack gunStack;
    private final ItemStack ammoStack;

    private GunAmmoQueryEntry(ItemStack gunStack, ItemStack ammoStack) {
        this.gunStack = gunStack;
        this.ammoStack = ammoStack;
    }

    public static List<GunAmmoQueryEntry> getAllGunAmmoQueryEntries() {
        List<GunAmmoQueryEntry> entries = new ArrayList<>();
        Map<ResourceLocation, ItemStack> ammoStacks = new HashMap<>();
        TimelessAPI.getAllCommonGunIndex().stream()
                .sorted(Comparator.<Map.Entry<ResourceLocation, CommonGunIndex>>comparingInt(entry -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    ResourceLocation ammoId = entry.getValue().getGunData().getAmmoId();
                    if (ammoId == null || TimelessAPI.getCommonAmmoIndex(ammoId).isEmpty()) {
                        return;
                    }
                    ItemStack gun = GunItemBuilder.create().setId(entry.getKey()).build();
                    if (!(gun.getItem() instanceof IGun)) {
                        return;
                    }
                    ItemStack ammo = ammoStacks.computeIfAbsent(ammoId, id -> AmmoItemBuilder.create().setId(id).build());
                    if (ammo.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gun, ammo)) {
                        entries.add(new GunAmmoQueryEntry(gun, ammo));
                    }
                });
        return entries;
    }

    public ItemStack getGunStack() {
        return gunStack;
    }

    public ItemStack getAmmoStack() {
        return ammoStack;
    }
}
