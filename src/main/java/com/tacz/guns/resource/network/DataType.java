package com.tacz.guns.resource.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.StringRepresentable;

public enum DataType implements StringRepresentable {
    /**
     * 需要同步到客户端的数据类型
     */
    GUN_DATA,
    ATTACHMENT_DATA,
    AMMO_INDEX,
    GUN_INDEX,
    ATTACHMENT_INDEX,
    RECIPES,
    RECIPE_FILTER,
    ATTACHMENT_TAGS,
    ALLOW_ATTACHMENT_TAGS,
    BLOCK_DATA,
    BLOCK_INDEX,
    ATTACHMENT_VARIANT_DATA,
    ATTACHMENT_VARIANT_INDEX,
    SLOT_ADAPTER_DATA,
    SLOT_ADAPTER_INDEX;

    public static final Codec<DataType> CODEC = StringRepresentable.fromEnum(DataType::values);
    @Override
    public String getSerializedName() { return name(); }
}
