package com.tacz.guns.resource.pojo.data.recipe;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;

public class GunResult {
    public static final Codec<GunResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("ammo_count", 0).forGetter(GunResult::getAmmoCount),
        Codec.unboundedMap(AttachmentType.CODEC, ResourceLocation.CODEC).optionalFieldOf("attachments", Maps.newEnumMap(AttachmentType.class)).xmap(map -> {
            EnumMap<AttachmentType, ResourceLocation> attachments = Maps.newEnumMap(AttachmentType.class);
            attachments.putAll(map);
            return attachments;
        }, map -> map).forGetter(GunResult::getAttachments)
    ).apply(instance, GunResult::new));

    public GunResult() { }

    public GunResult(int ammoCount, EnumMap<AttachmentType, ResourceLocation> attachments) {
        this.ammoCount = ammoCount;
        this.attachments = attachments;
    }

    @SerializedName("ammo_count")
    private int ammoCount = 0;

    @SerializedName("attachments")
    private EnumMap<AttachmentType, ResourceLocation> attachments = Maps.newEnumMap(AttachmentType.class);

    public int getAmmoCount() {
        return ammoCount;
    }

    public EnumMap<AttachmentType, ResourceLocation> getAttachments() {
        return attachments;
    }
}
