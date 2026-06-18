package com.tacz.guns.util;

import com.google.common.collect.Maps;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;

public final class DefaultGunAttachments {
    private DefaultGunAttachments() {
    }

    public static EnumMap<AttachmentType, ResourceLocation> defaults(ResourceLocation gunId) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> {
                    EnumMap<AttachmentType, ResourceLocation> defaults = Maps.newEnumMap(AttachmentType.class);
                    index.getGunData().getBuiltInAttachments().forEach((type, builtInAttachment) -> {
                        ResourceLocation defaultAttachment = builtInAttachment.getDefaultAttachment();
                        if (defaultAttachment != null) {
                            defaults.put(type, defaultAttachment);
                        }
                    });
                    return defaults;
                })
                .orElseGet(() -> Maps.newEnumMap(AttachmentType.class));
    }
}
