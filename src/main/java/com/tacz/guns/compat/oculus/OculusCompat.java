package com.tacz.guns.compat.oculus;

import com.tacz.guns.client.resource.texture.FilePackTexture;
import com.tacz.guns.client.resource.texture.ZipPackTexture;
import com.tacz.guns.compat.oculus.pbr.FilePackTexturePBRLoader;
import com.tacz.guns.compat.oculus.pbr.ZipPackTexturePBRLoader;
import com.tacz.guns.init.CompatRegistry;
import net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.irisshaders.iris.api.v0.IrisApi;
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
            PBRTextureLoaderRegistry.INSTANCE.register(FilePackTexture.class, new FilePackTexturePBRLoader());
            PBRTextureLoaderRegistry.INSTANCE.register(ZipPackTexture.class, new ZipPackTexturePBRLoader());
        }
    }
}
