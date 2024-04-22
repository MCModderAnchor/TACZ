package com.tac.guns.compat.oculus;

import net.coderbot.iris.pipeline.ShadowRenderer;
import net.minecraftforge.fml.ModList;

public final class OculusCompat {
    private static final String MOD_ID = "oculus";

    public static boolean isRenderShadow() {
        if (ModList.get().isLoaded(MOD_ID)) {
            return ShadowRenderer.ACTIVE;
        }
        return false;
    }
}
