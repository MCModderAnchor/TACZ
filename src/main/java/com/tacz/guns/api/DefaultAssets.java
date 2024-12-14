package com.tacz.guns.api;

import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;

public final class DefaultAssets {
    public static ResourceLocation DEFAULT_GUN_DISPLAY_ID = new ResourceLocation(GunMod.MOD_ID, "default");
    public static ResourceLocation EMPTY_GUN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");
    public static ResourceLocation EMPTY_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_BLOCK_ID = new ResourceLocation(GunMod.MOD_ID, "gun_smith_table");
    public static ResourceLocation EMPTY_BLOCK_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot");
    public static ResourceLocation EMPTY_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot_blue");
    public static ResourceLocation EMPTY_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static boolean isEmptyAttachmentId(ResourceLocation attachmentId) {
        return EMPTY_ATTACHMENT_ID.equals(attachmentId);
    }
}
