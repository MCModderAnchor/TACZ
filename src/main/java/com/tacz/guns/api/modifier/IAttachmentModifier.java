package com.tacz.guns.api.modifier;

import com.tacz.guns.resource.pojo.data.gun.GunData;

/**
 * 配件属性修改器
 *
 * @param <T> Json 读取的数据类型
 * @param <K> 配件缓存属性值
 */
public interface IAttachmentModifier<T, K> {
    /**
     * 配件属性修改器，同时也用于 Json 读取时作为字段名进行识别
     *
     * @return Json 读取时的字段名
     */
    String getId();

    /**
     * 从 Json 读取数据
     *
     * @param json 输入的 json 字符串
     * @return 读取后的 json 对象
     */
    JsonProperty<T, K> readJson(String json);

    /**
     * 初始化缓存
     *
     * @param gunData 枪械数据
     * @return 初始化读取的数据
     */
    CacheProperty<K> initCache(GunData gunData);
}