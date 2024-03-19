package com.tac.guns.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.tac.guns.api.client.render.ModifiableFrameBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements ModifiableFrameBuffer {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Shadow
    public abstract void resize(int width, int height, boolean clearError);

    private boolean isStencilBufferEnabled;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
            boolean useDepth,
            CallbackInfo ci
    ) {
        isStencilBufferEnabled = false;
    }

    @Redirect(
            method = "createBuffers(IIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
            )
    )
    private void redirectTexImage2d(
            int target, int level, int internalFormat,
            int width, int height,
            int border, int format, int type,
            IntBuffer pixels
    ) {
        if (internalFormat == GL_DEPTH_COMPONENT && isStencilBufferEnabled) {
            GlStateManager._texImage2D(
                    target,
                    level,
                    GL30.GL_DEPTH24_STENCIL8,
                    width,
                    height,
                    border,
                    GL30.GL_DEPTH_STENCIL,
                    GL30.GL_UNSIGNED_INT_24_8,
                    pixels
            );
        }
        else {
            GlStateManager._texImage2D(
                    target, level, internalFormat, width, height,
                    border, format, type, pixels
            );
        }
    }

    @Redirect(
            method = "createBuffers(IIZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V"
            )
    )
    private void redirectFrameBufferTexture2d(
            int target, int attachment, int textureTarget, int texture, int level
    ) {

        if (attachment == GL30C.GL_DEPTH_ATTACHMENT && isStencilBufferEnabled) {
            GlStateManager._glFramebufferTexture2D(
                    target, GL30.GL_DEPTH_STENCIL_ATTACHMENT, textureTarget, texture, level
            );
        }
        else {
            GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        }
    }

    @Override
    public void setStencilBufferEnabledAndReload(boolean enable) {
        if (isStencilBufferEnabled != enable) {
            isStencilBufferEnabled = enable;
            resize(width, height, Minecraft.ON_OSX);
        }
    }

    @Override
    public boolean getStencilBufferEnabled(){
        return isStencilBufferEnabled;
    }
}
