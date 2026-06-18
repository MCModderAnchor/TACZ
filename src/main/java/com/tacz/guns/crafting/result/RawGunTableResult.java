package com.tacz.guns.crafting.result;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import com.tacz.guns.util.DefaultGunAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;


/**
 * 配方加载时部分物品的上下文还未完成初始化<br/>
 * 等待到实际需要使用配方时再进行初始化
 */
public class RawGunTableResult {
    private final String type;
    private final int count;
    private final ResourceLocation id;
    @Nullable
    private GunResult extraData;
    @Nullable
    private CompoundTag nbt;

    public RawGunTableResult(@NotNull String type, @NotNull ResourceLocation id, int count) {
        this.type = type;
        this.id = id;
        this.count = count;
    }

    public void setExtraData(@Nullable GunResult extraData) {
        this.extraData = extraData;
    }

    public void setNbt(@Nullable CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static GunSmithTableResult init(RawGunTableResult raw) {
        GunSmithTableResult result = switch (raw.type) {
            case GunSmithTableResult.GUN -> raw.getGunStack();
            case GunSmithTableResult.AMMO -> raw.getAmmoStack();
            case GunSmithTableResult.ATTACHMENT -> raw.getAttachmentStack();
            default -> new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY);
        };
        if (raw.nbt != null) {
            CompoundTag itemTag = result.getResult().getOrCreateTag();
            for (String key : raw.nbt.getAllKeys()) {
                Tag tag = raw.nbt.get(key);
                if (tag != null) {
                    itemTag.put(key, tag);
                }
            }
        }
        return result;
    }

    private GunSmithTableResult getGunStack() {
        int ammoCount;
        EnumMap<AttachmentType, ResourceLocation> attachments;
        if (extraData != null) {
            ammoCount = Math.max(0, extraData.getAmmoCount());
            attachments = extraData.getAttachments();
        } else {
            ammoCount = 0;
            attachments = new EnumMap<>(AttachmentType.class);
        }

        return TimelessAPI.getCommonGunIndex(id).map(gunIndex -> {
            ItemStack itemStack = GunItemBuilder.create()
                    .setCount(count)
                    .setId(id)
                    .setAmmoCount(ammoCount)
                    .setAmmoInBarrel(false)
                    .putAllVirtualAttachment(DefaultGunAttachments.defaults(id))
                    .putAllAttachment(attachments)
                    .setFireMode(gunIndex.getGunData().getFireModeSet().get(0)).build();
            String raw = gunIndex.getType();
            if (!raw.contains(":")) {
                raw = GunMod.MOD_ID + ":" + raw;
            }
            ResourceLocation group = ResourceLocation.tryParse(raw);
            return new GunSmithTableResult(itemStack, group);
        }).orElse(new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY));
    }

    private GunSmithTableResult getAmmoStack() {
        return new GunSmithTableResult(AmmoItemBuilder.create().setCount(count).setId(id).build(), TabConfig.TAB_AMMO);
    }

    private GunSmithTableResult getAttachmentStack() {
        return TimelessAPI.getCommonAttachmentIndex(id).map(attachmentIndex -> {
            ItemStack itemStack = AttachmentItemBuilder.create().setCount(count).setId(id).build();
            String raw = attachmentIndex.getType().name().toLowerCase(Locale.US);
            if (!raw.contains(":")) {
                raw = GunMod.MOD_ID + ":" + raw;
            }
            ResourceLocation group = ResourceLocation.tryParse(raw);
            return new GunSmithTableResult(itemStack, group);
        }).orElse(new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY));
    }
}
