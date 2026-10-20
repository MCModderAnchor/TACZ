package com.tacz.guns.compat.iris;

import net.minecraft.client.renderer.MultiBufferSource;

/** Exposes the actual Iris buffer so stencil passes can flush before changing render state. */
public interface IUnflushableWrapper {
    MultiBufferSource tacz$getWrapped();
}
