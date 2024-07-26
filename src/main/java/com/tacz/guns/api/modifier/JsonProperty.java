package com.tacz.guns.api.modifier;

import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

/**
 * 配件从 Json 读取的数据
 *
 * @param <T> Json 读取的数据类型
 * @param <K> 配件缓存属性值
 */
public abstract class JsonProperty<T, K> {
    protected List<Component> components = Lists.newArrayList();
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

    public List<Component> getComponents() {
        return components;
    }

    /**
     * 初始化文本提示，用于配件的描述文本
     */
    public abstract void initComponents();

    /**
     * 依据 Json 读取的数据类型，对缓存数据进行计算
     *
     * @param gunItem 当前持有的枪械
     * @param gunData 枪械数据
     * @param cache   缓存数据
     */
    public abstract void eval(ItemStack gunItem, GunData gunData, CacheProperty<K> cache);
}
