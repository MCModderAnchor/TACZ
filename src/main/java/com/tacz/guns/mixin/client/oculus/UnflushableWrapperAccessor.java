package com.tacz.guns.mixin.client.oculus;

import com.tacz.guns.compat.oculus.IUnflushableWrapper;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 访问iris的FullyBufferedMultiBufferSource$UnflushableWrapper，用于渲染模板缓冲时强行flush
@Mixin(targets = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource$UnflushableWrapper")
public abstract class UnflushableWrapperAccessor implements IUnflushableWrapper {
    @Accessor(value = "wrapped", remap = false)
    public abstract FullyBufferedMultiBufferSource tacz$getWrappedInternal();

    @Override
    public MultiBufferSource tacz$getWrapped() {
        return tacz$getWrappedInternal();
    }
}