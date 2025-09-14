package com.tacz.guns.compat.realcamera;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraftforge.fml.ModList;

public class RealCameraCompatHolder {
    public static boolean hasMod() {
        return ModList.get().isLoaded("realcamera");
    }

    public static float getCompatCrosshairX(float x) {
        if (RealCameraCore.isActive()) {
            x += (float) CrosshairUtil.offset.x();
        }
        return x;
    }

    public static float getCompatCrosshairY(float y) {
        if (RealCameraCore.isActive()) {
            y -= (float) CrosshairUtil.offset.y();
        }
        return y;
    }
}