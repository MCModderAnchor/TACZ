package com.tacz.guns.api.item.gun;

import com.google.common.collect.Maps;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.Map;

public class GunItemManager {
    private static final Map<String, RegistryObject<? extends AbstractGunItem>> GUN_ITEM_MAP = Maps.newHashMap();

    /**
     * 建议在 RegistryEvent.Register<Item> 事件时注册此枪械变种
     */
    public static void registerGunItem(String name, RegistryObject<? extends AbstractGunItem> registryObject) {
        GUN_ITEM_MAP.put(name, registryObject);
    }

    public static RegistryObject<? extends AbstractGunItem> getGunItemRegistryObject(String key) {
        return GUN_ITEM_MAP.get(key);
    }

    public static Collection<RegistryObject<? extends AbstractGunItem>> getAllGunItems() {
        return GUN_ITEM_MAP.values();
    }
}
