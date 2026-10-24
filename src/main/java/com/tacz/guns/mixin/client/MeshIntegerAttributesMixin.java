package com.tacz.guns.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.gpu.MeshIntegerAttributes;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Backport SBM 2.6.1's AMD upload fix while the NeoForge port uses SBM 2.6.0. */
@Mixin(value = MeshIntegerAttributes.class, remap = false)
public abstract class MeshIntegerAttributesMixin {
    @Shadow @Final static int TABLE_BYTES;

    @Inject(method = "ensureReady", at = @At("HEAD"))
    private void tacz$assertRenderThread(CallbackInfoReturnable<Boolean> cir) {
        RenderSystem.assertOnRenderThread();
    }

    // The original buffer-size query can crash inside AMD's driver. A failed upload still disables
    // the backend through SBM's existing error path. Newer SBM versions omit this query entirely.
    @Redirect(method = "ensureReady", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL15;glGetBufferParameteri(II)I"), require = 0)
    private int tacz$checkUploadError(int target, int parameter) {
        return GL11.glGetError() == GL11.GL_NO_ERROR ? TABLE_BYTES : -1;
    }
}
