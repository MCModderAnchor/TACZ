package com.tacz.guns.mixin.client.iris;

import com.tacz.guns.compat.iris.IUnflushableWrapper;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource$UnflushableWrapper", remap = false)
public abstract class UnflushableWrapperAccessor implements IUnflushableWrapper {
    @Accessor("wrapped")
    public abstract FullyBufferedMultiBufferSource tacz$getWrappedInternal();

    @Override
    public MultiBufferSource tacz$getWrapped() {
        return tacz$getWrappedInternal();
    }
}
