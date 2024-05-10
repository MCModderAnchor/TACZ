package com.tacz.guns.api.item.gun;

import com.google.common.collect.Maps;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class GunItemManager {
    private static final Map<String, RegistryObject<? extends AbstractGunItem>> gunItemMap = Maps.newHashMap();

    public static void registerGunItem(String key, RegistryObject<? extends AbstractGunItem> registryObject) {
        gunItemMap.put(key, registryObject);
    }

    public static RegistryObject<? extends AbstractGunItem> getGunItemRegistryObject(String key) {
        return gunItemMap.get(key);
    }
}
