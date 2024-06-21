package com.tacz.guns.resource.serialize;

import com.google.common.reflect.TypeToken;
import com.google.gson.InstanceCreator;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.EnumMap;

/**
 * From: https://stackoverflow.com/questions/16127904/gson-fromjson-return-linkedhashmap-instead-of-enummap
 */
public class EnumMapInstanceCreator<K extends Enum<K>, V> implements InstanceCreator<EnumMap<K, V>> {
    private final Class<K> enumClazz;

    public EnumMapInstanceCreator(final Class<K> enumClazz) {
        super();
        this.enumClazz = enumClazz;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Type getType() {
        return new TypeToken<EnumMap<AttachmentType, ResourceLocation>>() {
        }.getType();
    }

    public static EnumMapInstanceCreator<AttachmentType, ResourceLocation> getInstance() {
        return new EnumMapInstanceCreator<>(AttachmentType.class);
    }

    @Override
    public EnumMap<K, V> createInstance(final Type type) {
        return new EnumMap<K, V>(enumClazz);
    }
}
