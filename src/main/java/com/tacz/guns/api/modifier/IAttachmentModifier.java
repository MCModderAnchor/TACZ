package com.tacz.guns.api.modifier;

import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

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
     * 可选字段，为了兼容旧版本 json 文件设立此方法
     *
     * @return 旧版本的配件属性修改的 json 字段名
     */
    default String getOptionalFields() {
        return StringUtils.EMPTY;
    }

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
     * @param gunItem 当前枪械物品
     * @param gunData 枪械数据
     * @return 初始化读取的数据
     */
    CacheProperty<K> initCache(ItemStack gunItem, GunData gunData);

    /**
     * 用于渲染改装界面的配置属性
     */
    @OnlyIn(Dist.CLIENT)
    void renderPropertyDiagrams(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty,
                                int barStartX, int barEndX, int barMaxWidth,
                                int barBackgroundColor, int barBaseColor, int barPositivelyColor, int barNegativeColor,
                                int fontColor, int nameTextStartX, int valueTextStartX,
                                GuiGraphics graphics, Font font, int yOffset
    );

    /**
     * 用于渲染渲染改装界面的配置属性的 YOffset 偏移
     *
     * @return YOffset 偏移
     */
    @OnlyIn(Dist.CLIENT)
    default int getYOffset() {
        return 10;
    }
}