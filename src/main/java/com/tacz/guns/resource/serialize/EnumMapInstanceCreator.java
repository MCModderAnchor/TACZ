package com.tacz.guns.resource.serialize;

import com.google.common.reflect.TypeToken;
import com.google.gson.InstanceCreator;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

/**
 * From: https://stackoverflow.com/questions/16127904/gson-fromjson-return-linkedhashmap-instead-of-enummap
 */
public class EnumMapInstanceCreator<K extends Enum<K>, V> implements InstanceCreator<EnumMap<K, V>> {
    private final Class<K> enumClazz;

    public EnumMapInstanceCreator(final Class<K> enumClazz) {
        super();
        this.enumClazz = enumClazz;
    }

    @Override
    public EnumMap<K, V> createInstance(final Type type) {
        return new EnumMap<K, V>(enumClazz);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Type getAttachmentType() {
        return new TypeToken<EnumMap<AttachmentType, ResourceLocation>>() {
        }.getType();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Type getFireModeType() {
        return new TypeToken<Map<FireMode, GunFireModeAdjustData>>() {
        }.getType();
    }

    public static EnumMapInstanceCreator<AttachmentType, ResourceLocation> getAttachmentInstance() {
        return new EnumMapInstanceCreator<>(AttachmentType.class);
    }

    public static EnumMapInstanceCreator<FireMode, GunFireModeAdjustData> getFireModeInstance() {
        return new EnumMapInstanceCreator<>(FireMode.class);
    }
}
