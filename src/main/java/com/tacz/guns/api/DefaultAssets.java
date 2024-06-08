package com.tacz.guns.api;

import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;

public final class DefaultAssets {
    public static ResourceLocation EMPTY_GUN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");
    public static ResourceLocation DEFAULT_GUN_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "ak47_display");

    public static ResourceLocation DEFAULT_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");
    public static ResourceLocation DEFAULT_AMMO_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "762x39_display");
    public static ResourceLocation EMPTY_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot");
    public static ResourceLocation EMPTY_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot_blue");
    public static ResourceLocation EMPTY_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");
}
