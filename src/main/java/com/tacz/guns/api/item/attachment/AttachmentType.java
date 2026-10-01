package com.tacz.guns.api.item.attachment;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Locale;

public enum AttachmentType implements StringRepresentable {
    /**
     * 瞄具
     */
    @SerializedName("scope")
    SCOPE,
    /**
     * 枪口组件
     */
    @SerializedName("muzzle")
    MUZZLE,
    /**
     * 枪托
     */
    @SerializedName("stock")
    STOCK,
    /**
     * 握把
     */
    @SerializedName("grip")
    GRIP,
    /**
     * 激光指示器
     */
    @SerializedName("laser")
    LASER,
    /**
     * 扩容弹夹（匣）
     */
    @SerializedName("extended_mag")
    EXTENDED_MAG,
    /**
     * 用来表示物品不是配件的情况。
     */
    NONE;

    public static final Codec<AttachmentType> CODEC = StringRepresentable.fromEnum(AttachmentType::values);

    private final String serializedName;

    AttachmentType() {
        String name = name().toLowerCase(Locale.US);
        try {
            Field field = getClass().getField(name());
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName != null) name = serializedName.value();
        } catch (NoSuchFieldException ignored) { }
        this.serializedName = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }

    public static AttachmentType fromId(int id) {
        return VALUES[id];
    }

    private static final AttachmentType[] VALUES = AttachmentType.values();
}
