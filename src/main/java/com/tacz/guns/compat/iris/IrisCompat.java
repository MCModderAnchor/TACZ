package com.tacz.guns.compat.iris;

import com.tacz.guns.init.CompatRegistry;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;

public final class IrisCompat {
    public static void initCompat() { }

    public static boolean isPackInUseQuick() {
        if (ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return IrisCompatInner.isPackInUseQuick();
        }
        return false;
    }

    public static boolean isRenderShadow() {
        if (ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return IrisCompatInner.isRenderShadow();
        }
        return false;
    }

    public static boolean isUsingRenderPack() {
        if (ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return IrisApi.getInstance().isShaderPackInUse();
        }
        return false;
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        if (ModList.get().isLoaded(CompatRegistry.IRIS)) {
            return IrisCompatInner.endBatch(bufferSource);
        }
        return false;
    }
}
