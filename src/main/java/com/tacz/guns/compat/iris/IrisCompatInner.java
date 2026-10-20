package com.tacz.guns.compat.iris;

import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.MultiBufferSource;

public class IrisCompatInner {
    public static MultiBufferSource getHandBufferSource() {
        var renderer = net.irisshaders.iris.pathways.HandRenderer.INSTANCE;
        return Iris.isPackInUseQuick() && renderer.isActive() ? renderer.getBufferSource() : null;
    }

    public static boolean isPackInUseQuick() {
        return Iris.isPackInUseQuick();
    }

    public static boolean isRenderShadow() {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    public static boolean endBatch(MultiBufferSource bufferSource) {
        if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource) {
            fullyBufferedMultiBufferSource.endBatch();
            return true;
        }
        return false;
    }
}
