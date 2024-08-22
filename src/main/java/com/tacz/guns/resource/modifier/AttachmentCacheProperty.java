package com.tacz.guns.resource.modifier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.world.item.ItemStack;


import java.util.List;
import java.util.Map;

/**
 * 所有与配件缓存计算相关的都在这里
 */
public class AttachmentCacheProperty {
    @SuppressWarnings("rawtypes")
    private final Map<String, CacheValue> cacheValues = Maps.newHashMap();
    private final Map<String, List<?>> cacheModifiers = Maps.newHashMap();

    @SuppressWarnings("all")
    public void eval(ItemStack gunItem, GunData gunData) {
        // 数值初始化
        var modifiers = AttachmentPropertyManager.getModifiers();
        modifiers.forEach((id, value) -> {
            cacheValues.put(id, value.initCache(gunItem, gunData));
            cacheModifiers.put(id, Lists.newArrayList());
        });

        // 逐个读取配件属性，写入 modifier
        AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, data -> {
            data.getModifier().forEach((id, value) -> {
                List objects = cacheModifiers.get(id);
                objects.add(value.getValue());
            });
        });

        // 最后一次性计算完毕，并存入缓存
        cacheValues.forEach((id, value) -> {
            List cacheModifier = cacheModifiers.get(id);
            // 可能该枪没有这个 modifier 或者 modifier 为空
            if (cacheModifier == null || cacheModifier.isEmpty()) {
                return;
            }
            modifiers.get(id).eval(cacheModifier, value);
        });

        // 清除不必要的数据，防止内存占用
        cacheModifiers.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> T getCache(String id) {
        return (T) cacheValues.get(id).getValue();
    }
}
