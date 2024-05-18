package com.tacz.guns.compat.oculus;

import com.tacz.guns.compat.oculus.pbr.PBRRegister;
import com.tacz.guns.init.CompatRegistry;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.fml.ModList;

public final class OculusCompat {
    public static boolean isRenderShadow() {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        }
        return false;
    }

    public static boolean isUsingRenderPack() {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            return IrisApi.getInstance().isShaderPackInUse();
        }
        return false;
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource) {
                fullyBufferedMultiBufferSource.endBatch();
                return true;
            }
        }
        return false;
    }

    public static void registerPBRLoader() {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            PBRRegister.registerPBRLoader();
        }
    }
}
