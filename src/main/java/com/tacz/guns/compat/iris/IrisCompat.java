package com.tacz.guns.compat.iris;

import com.tacz.guns.init.CompatRegistry;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;

public final class IrisCompat {
    public static void initCompat() { }

    public static MultiBufferSource getHandBufferSource() {
        return ModList.get().isLoaded(CompatRegistry.IRIS) ? IrisCompatInner.getHandBufferSource() : null;
    }

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

    public static boolean endBatch(MultiBufferSource bufferSource) {
        if (ModList.get().isLoaded(CompatRegistry.IRIS)) {
            // Iris 的手部 pass 使用 UnflushableWrapper，其 endBatch 是空操作。
            // 取出实际缓冲，确保模板状态切换前完成绘制。
            if (bufferSource instanceof IUnflushableWrapper wrapper) {
                bufferSource = wrapper.tacz$getWrapped();
            }
            return IrisCompatInner.endBatch(bufferSource);
        }
        return false;
    }
}
