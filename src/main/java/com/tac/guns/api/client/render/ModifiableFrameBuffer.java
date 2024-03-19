package com.tac.guns.api.client.render;

public interface ModifiableFrameBuffer {
    void setStencilBufferEnabledAndReload(boolean enable);

    boolean getStencilBufferEnabled();
}
