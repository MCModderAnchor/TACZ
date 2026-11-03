package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 一种弹药的适用枪械的一页查询结果。
 */
public class AmmoQueryEntry {
    public static final int GUNS_PER_PAGE = 54;

    private final ItemStack ammoStack;
    private final List<ItemStack> gunStacks;
    private final int totalGunCount;
    private final int pageNumber;
    private final int pageCount;

    private AmmoQueryEntry(ItemStack ammoStack, List<ItemStack> gunStacks, int totalGunCount, int pageNumber, int pageCount) {
        this.ammoStack = ammoStack;
        this.gunStacks = List.copyOf(gunStacks);
        this.totalGunCount = totalGunCount;
        this.pageNumber = pageNumber;
        this.pageCount = pageCount;
    }

    public static List<AmmoQueryEntry> getAllAmmoQueryEntries(List<GunAmmoQueryEntry> gunAmmoEntries) {
        // 双向查询共用同一份对应关系，避免两个方向的结果不一致。
        Map<ResourceLocation, List<GunAmmoQueryEntry>> gunsByAmmo = new HashMap<>();
        for (GunAmmoQueryEntry entry : gunAmmoEntries) {
            IAmmo iAmmo = IAmmo.getIAmmoOrNull(entry.getAmmoStack());
            if (iAmmo != null) {
                ResourceLocation ammoId = iAmmo.getAmmoId(entry.getAmmoStack());
                gunsByAmmo.computeIfAbsent(ammoId, id -> new ArrayList<>()).add(entry);
            }
        }

        List<AmmoQueryEntry> entries = new ArrayList<>();
        TimelessAPI.getAllCommonAmmoIndex().stream()
                .sorted(Comparator.<Map.Entry<ResourceLocation, CommonAmmoIndex>>comparingInt(entry -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    List<GunAmmoQueryEntry> matches = gunsByAmmo.get(entry.getKey());
                    if (matches == null || matches.isEmpty()) {
                        return;
                    }
                    ItemStack ammo = matches.get(0).getAmmoStack();
                    List<ItemStack> guns = matches.stream().map(GunAmmoQueryEntry::getGunStack).toList();
                    int total = guns.size();
                    int pageCount = (total + GUNS_PER_PAGE - 1) / GUNS_PER_PAGE;
                    for (int page = 0; page < pageCount; page++) {
                        int start = page * GUNS_PER_PAGE;
                        int end = Math.min(start + GUNS_PER_PAGE, total);
                        entries.add(new AmmoQueryEntry(ammo, guns.subList(start, end), total, page + 1, pageCount));
                    }
                });
        return entries;
    }

    public ItemStack getAmmoStack() {
        return ammoStack;
    }

    public List<ItemStack> getGunStacks() {
        return gunStacks;
    }

    public int getTotalGunCount() {
        return totalGunCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageCount() {
        return pageCount;
    }
}
