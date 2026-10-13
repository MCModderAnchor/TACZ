package com.tacz.guns.compat.oculus.legacy;

import net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.MultiBufferSource;

public final class OculusCompatLegacy {
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
