package com.tacz.guns.api.modifier;

import com.tacz.guns.resource.pojo.data.gun.GunData;

/**
 * 配件从 Json 读取的数据
 *
 * @param <T> Json 读取的数据类型
 * @param <K> 配件缓存属性值
 */
public abstract class JsonProperty<T, K> {
    private T value;

    public JsonProperty(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    /**
     * 依据 Json 读取的数据类型，对缓存数据进行计算
     *
     * @param gunData 枪械数据
     * @param cache   缓存数据
     */
    public abstract void eval(GunData gunData, CacheProperty<K> cache);
}
