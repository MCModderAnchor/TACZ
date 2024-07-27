package com.tacz.guns.api.modifier;

/**
 * 单个的配件缓存属性值
 */
public class CacheValue<T> {
    private T value;

    public CacheValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
