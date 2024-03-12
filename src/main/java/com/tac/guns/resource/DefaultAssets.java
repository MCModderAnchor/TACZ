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

    public static String SHOOT_SOUND = "shoot";
    public static String DRY_FIRE_SOUND = "dry_fire";
    public static String RELOAD_EMPTY_SOUND = "reload_empty";
    public static String RELOAD_TACTICAL_SOUND = "reload_tactical";
    public static String INSPECT_EMPTY_SOUND = "inspect_empty";
    public static String INSPECT_SOUND = "inspect";
    public static String DRAW_SOUND = "draw";
}
