package com.tacz.guns.compat.oculus;

import com.tacz.guns.compat.oculus.legacy.OculusCompatLegacy;
import com.tacz.guns.compat.oculus.newly.OculusCompatNewly;
import com.tacz.guns.init.CompatRegistry;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.function.Function;
import java.util.function.Supplier;

public final class OculusCompat {
    private static final DefaultArtifactVersion VERSION = new DefaultArtifactVersion("1.7.0");
    private static Function<MultiBufferSource.BufferSource, Boolean> END_BATCH_FUNCTION;
    private static Supplier<Boolean> IS_RENDER_SHADOW_SUPPER;

    public static void initCompat() {
        ModList.get().getModContainerById(CompatRegistry.OCULUS).ifPresent(mod -> {
            if (mod.getModInfo().getVersion().compareTo(VERSION) >= 0) {
                END_BATCH_FUNCTION = OculusCompatNewly::endBatch;
                IS_RENDER_SHADOW_SUPPER = OculusCompatNewly::isRenderShadow;
                OculusCompatNewly.registerPBRLoader();
            } else {
                END_BATCH_FUNCTION = OculusCompatLegacy::endBatch;
                IS_RENDER_SHADOW_SUPPER = OculusCompatLegacy::isRenderShadow;
                OculusCompatLegacy.registerPBRLoader();
            }
        });
    }

    public static boolean isRenderShadow() {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            return IS_RENDER_SHADOW_SUPPER.get();
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
            return END_BATCH_FUNCTION.apply(bufferSource);
        }
        return false;
    }
}
