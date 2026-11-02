package com.tacz.guns.compat.oculus;

import com.tacz.guns.compat.oculus.legacy.OculusCompatLegacy;
import com.tacz.guns.compat.oculus.newly.OculusCompatNewly;
import com.tacz.guns.init.CompatRegistry;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OculusCompat {
    private static final DefaultArtifactVersion VERSION = new DefaultArtifactVersion("1.7.0");
    private static Function<MultiBufferSource, Boolean> END_BATCH_FUNCTION;
    private static Supplier<Boolean> IS_RENDER_SHADOW_SUPPER;
    private static Supplier<MultiBufferSource> HAND_BUFFER_SOURCE;

    public static void initCompat() {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        ModList.get().getModContainerById(CompatRegistry.OCULUS).ifPresent(mod -> {
            if (mod.getModInfo().getVersion().compareTo(VERSION) >= 0) {
                END_BATCH_FUNCTION = OculusCompatNewly::endBatch;
                IS_RENDER_SHADOW_SUPPER = OculusCompatNewly::isRenderShadow;
                HAND_BUFFER_SOURCE = OculusCompatNewly::getHandBufferSource;
            } else {
                END_BATCH_FUNCTION = OculusCompatLegacy::endBatch;
                IS_RENDER_SHADOW_SUPPER = OculusCompatLegacy::isRenderShadow;
                HAND_BUFFER_SOURCE = OculusCompatLegacy::getHandBufferSource;
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

    public static boolean endBatch(MultiBufferSource bufferSource) {
        if (ModList.get().isLoaded(CompatRegistry.OCULUS)) {
            // Iris 第一人称手部 pass 传入的是 UnflushableWrapper，其 endBatch 是空操作；
            // 经 mixin 注入 IUnflushableWrapper 后取出内部真正的 FullyBufferedMultiBufferSource，
            // 再交给版本分派 flush（保证模板状态与实际绘制同步）
            if (bufferSource instanceof IUnflushableWrapper wrapper) {
                bufferSource = wrapper.tacz$getWrapped();
            }
            return END_BATCH_FUNCTION.apply(bufferSource);
        }
        return false;
    }

    /** Compatibility fallback for render entry points without an explicit buffer. */
    @Nullable
    public static MultiBufferSource getHandBufferSource() {
        return ModList.get().isLoaded(CompatRegistry.OCULUS) && HAND_BUFFER_SOURCE != null
                ? HAND_BUFFER_SOURCE.get() : null;
    }
}
