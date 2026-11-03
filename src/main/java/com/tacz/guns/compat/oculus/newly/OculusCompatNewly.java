package com.tacz.guns.compat.oculus.newly;

import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.MultiBufferSource;

public final class OculusCompatNewly {
    public static MultiBufferSource getHandBufferSource() {
        return HandRenderer.INSTANCE.isActive() ? HandRenderer.INSTANCE.getBufferSource() : null;
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
