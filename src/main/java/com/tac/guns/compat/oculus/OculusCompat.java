package com.tac.guns.compat.oculus;

import net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.fml.ModList;

public final class OculusCompat {
    private static final String MOD_ID = "oculus";

    public static boolean isRenderShadow() {
        if (ModList.get().isLoaded(MOD_ID)) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        }
        return false;
    }

    public static boolean isUsingRenderPack() {
        if (ModList.get().isLoaded(MOD_ID)) {
            return IrisApi.getInstance().isShaderPackInUse();
        }
        return false;
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        if (ModList.get().isLoaded(MOD_ID)) {
            if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource) {
                fullyBufferedMultiBufferSource.endBatch();
                return true;
            }
        }
        return false;
    }
}
