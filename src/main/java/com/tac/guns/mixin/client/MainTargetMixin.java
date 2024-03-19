package com.tac.guns.mixin.client;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.tac.guns.api.client.render.ModifiableFrameBuffer;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;

@Mixin(MainTarget.class)
public abstract class MainTargetMixin extends RenderTarget {
    public MainTargetMixin(boolean pUseDepth) {
        super(pUseDepth);
        throw new RuntimeException();
    }

    @Redirect(
            method = "allocateDepthAttachment(Lcom/mojang/blaze3d/pipeline/MainTarget$Dimension;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"
            )
    )
    private void onTexImage2D(
            int target, int level, int internalFormat,
            int width, int height, int border, int format, int type, IntBuffer pixels
    ) {
        boolean isStencilBufferEnabled = ((ModifiableFrameBuffer) this).getStencilBufferEnabled();

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
            method = "createFrameBuffer(II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V"
            )
    )
    private void redirectFrameBufferTexture2d(
            int target, int attachment, int textureTarget, int texture, int level
    ) {
        boolean isStencilBufferEnabled = ((ModifiableFrameBuffer) this).getStencilBufferEnabled();

        if (attachment == GL30C.GL_DEPTH_ATTACHMENT && isStencilBufferEnabled) {
            GlStateManager._glFramebufferTexture2D(
                    target, GL30.GL_DEPTH_STENCIL_ATTACHMENT, textureTarget, texture, level
            );
        }
        else {
            GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        }
    }
}
