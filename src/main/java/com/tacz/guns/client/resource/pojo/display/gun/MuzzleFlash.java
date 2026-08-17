package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class MuzzleFlash {
    public enum Type {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("renew")
        RENEW
    }

    /**
     * 枪口效果类型：default 走旧的四边形火光渲染；renew 使用第一人称 snowstorm 粒子系统。
     */
    @SerializedName("type")
    private Type type = Type.DEFAULT;

    @SerializedName("texture")
    protected ResourceLocation texture = null;

    @SerializedName("scale")
    private float scale = 1;

    /**
     * renew 模式下使用的粒子效果定义 id（对应 assets/&lt;namespace&gt;/particle_definitions/ 下的定义）。
     * 留空时按枪械类型自动选择内置预设。
     */
    @SerializedName("texture_set")
    private String textureSet = null;

    public Type getType() {
        return type == null ? Type.DEFAULT : type;
    }

    public boolean isRenew() {
        return getType() == Type.RENEW;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getScale() {
        return scale;
    }

    public ResourceLocation getTextureSet() {
        return textureSet == null ? null : ResourceLocation.tryParse(textureSet);
    }
}