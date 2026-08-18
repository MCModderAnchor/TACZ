package com.tacz.guns.compat.oculus;

import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 由 {@code UnflushableWrapperAccessor} mixin 注入到 Iris 的
 * {@code FullyBufferedMultiBufferSource$UnflushableWrapper} 上，
 * 用于取出其内部真正的 {@code BufferSource} 以执行有效的 flush。
 */
public interface IUnflushableWrapper {
    MultiBufferSource.BufferSource tacz$getWrapped();
}