package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class PlayerAnimatorCompat {
    public static ResourceLocation LOWER_ANIMATION = new ResourceLocation(GunMod.MOD_ID, "lower_animation");
    public static ResourceLocation LOOP_UPPER_ANIMATION = new ResourceLocation(GunMod.MOD_ID, "loop_upper_animation");
    public static ResourceLocation ONCE_UPPER_ANIMATION = new ResourceLocation(GunMod.MOD_ID, "once_upper_animation");
    public static ResourceLocation ROTATION_ANIMATION = new ResourceLocation(GunMod.MOD_ID, "rotation");

    private static final String MOD_ID = "playeranimator";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
        if (isInstalled()) {
            AnimationDataRegisterFactory.registerData();
            MinecraftForge.EVENT_BUS.register(new AnimationManager());
        }
    }

    public static boolean loadAnimationFromZip(ZipFile zipFile, String zipPath) {
        if (isInstalled()) {
            return PlayerAnimatorLoader.load(zipFile, zipPath);
        }
        return false;
    }

    public static void loadAnimationFromFile(File file) {
        if (isInstalled()) {
            PlayerAnimatorLoader.load(file);
        }
    }

    public static void clearAllAnimationCache() {
        if (isInstalled()) {
            PlayerAnimatorAssetManager.get().clearAll();
        }
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity livingEntity, GunDisplayInstance display) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer) {
            return AnimationManager.hasPlayerAnimator3rd(display);
        }
        return false;
    }

    public static void stopAllAnimation(LivingEntity livingEntity) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.stopAllAnimation(player);
        }
    }

    public static void stopAllAnimation(LivingEntity livingEntity, int fadeTime) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.stopAllAnimation(player, fadeTime);
        }
    }

    public static void playAnimation(LivingEntity livingEntity, GunDisplayInstance display, float limbSwingAmount) {
        if (isInstalled() && livingEntity instanceof AbstractClientPlayer player) {
            AnimationManager.playLowerAnimation(player, display, limbSwingAmount);
            AnimationManager.playLoopUpperAnimation(player, display, limbSwingAmount);
            AnimationManager.playRotationAnimation(player, display);
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static void registerReloadListener(Consumer<PreparableReloadListener> register) {
        if (isInstalled()) {
            register.accept(PlayerAnimatorAssetManager.get());
        }
    }
}
