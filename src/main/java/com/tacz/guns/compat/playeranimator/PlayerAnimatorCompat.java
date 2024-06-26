package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.util.zip.ZipFile;

public class PlayerAnimatorCompat {
    public static ResourceLocation ANIMATION_DATA_ID = new ResourceLocation(GunMod.MOD_ID, "animation");

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

    public static void clearAllAnimations() {
        if (isInstalled()) {
            PlayerAnimatorAssetManager.INSTANCE.clearAll();
        }
    }

    public static boolean onHoldOrAim(AbstractClientPlayer player, ClientGunIndex gunIndex) {
        if (isInstalled()) {
            return AnimationManager.onHoldOrAim(player, gunIndex);
        }
        return false;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }
}
