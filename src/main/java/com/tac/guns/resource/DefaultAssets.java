package com.tac.guns.resource;

import com.tac.guns.GunMod;
import net.minecraft.resources.ResourceLocation;

public final class DefaultAssets {
    public static ResourceLocation DEFAULT_GUN_ID = new ResourceLocation(GunMod.MOD_ID, "ak47");
    public static ResourceLocation EMPTY_GUN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");
    public static ResourceLocation DEFAULT_GUN_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "ak47_display");
    public static ResourceLocation DEFAULT_GUN_DATA = new ResourceLocation(GunMod.MOD_ID, "ak47_data");

    public static ResourceLocation DEFAULT_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");
    public static ResourceLocation EMPTY_AMMO_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "lpvo_1_6");
    public static ResourceLocation EMPTY_ATTACHMENT_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

    public static ResourceLocation DEFAULT_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "lpvo_1_6_blue");
    public static ResourceLocation EMPTY_ATTACHMENT_SKIN_ID = new ResourceLocation(GunMod.MOD_ID, "empty");

}
