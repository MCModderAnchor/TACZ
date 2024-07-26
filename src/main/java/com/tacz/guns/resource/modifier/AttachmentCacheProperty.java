package com.tacz.guns.resource.modifier;

import com.google.common.collect.Maps;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 所有与配件缓存计算相关的都在这里
 */
public class AttachmentCacheProperty {
    @SuppressWarnings("rawtypes")
    private final Map<String, CacheProperty> cacheProperties = Maps.newHashMap();

    public AttachmentCacheProperty(ItemStack gunItem, GunData gunData) {
        var modifiers = AttachmentPropertyManager.getModifiers();
        modifiers.forEach((id, value) -> cacheProperties.put(id, value.initCache(gunItem, gunData)));
    }

    @SuppressWarnings("all")
    public void eval(ItemStack gunItem, GunData gunData, AttachmentData data) {
        data.getModifier().forEach((id, value) -> value.eval(gunItem, gunData, cacheProperties.get(id)));
    }

    @SuppressWarnings("unchecked")
    public <T> T getCache(String id) {
        return (T) cacheProperties.get(id).getValue();
    }
}
