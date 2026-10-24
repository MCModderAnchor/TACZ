package com.tacz.guns.client.renderer.scope;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;

/** GPU distance-to-ocular-boundary lookup, shared by scopes drawn sequentially in the lens pass. */
@OnlyIn(Dist.CLIENT)
final class ScopeLensContour {
    private static final int MAX_SIZE = 512;
    private static final TextureTarget[] TARGETS = new TextureTarget[2];
    private static ShaderInstance maskShader;
    private static ShaderInstance seedShader;
    private static ShaderInstance distanceShader;

    private ScopeLensContour() {
    }

    static void registerShaders(RegisterShadersEvent event) {
        maskShader = null;
        seedShader = null;
        distanceShader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope_lens_mask"), DefaultVertexFormat.POSITION),
                    instance -> maskShader = instance);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope_lens_seed"), DefaultVertexFormat.POSITION),
                    instance -> seedShader = instance);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope_lens_distance"), DefaultVertexFormat.POSITION),
                    instance -> distanceShader = instance);
        } catch (IOException exception) {
            GunMod.LOGGER.error("Could not load scope contour shaders; using elliptical lens edges", exception);
        }
    }

    /** Called inside ScopeLensRenderer's RenderState, before its target is attached to scene colour. */
    static Lookup prepare(TextureTarget ocular, int stencil, int screenWidth, int screenHeight,
                          float minU, float minV, float maxU, float maxV) {
        if (maskShader == null || seedShader == null || distanceShader == null) return null;
        // Padding supplies exterior seeds without treating the eye-box or screen edge as lens geometry.
        int x = (int) Math.max(0, Math.min(screenWidth, Math.floor((double) minU * screenWidth) - 2));
        int y = (int) Math.max(0, Math.min(screenHeight, Math.floor((double) minV * screenHeight) - 2));
        int right = (int) Math.max(0, Math.min(screenWidth, Math.ceil((double) maxU * screenWidth) + 2));
        int top = (int) Math.max(0, Math.min(screenHeight, Math.ceil((double) maxV * screenHeight) + 2));
        int width = right - x;
        int height = top - y;
        if (width <= 0 || height <= 0) return null;
        float scale = Math.min(1f, (float) MAX_SIZE / Math.max(width, height));
        int mapWidth = Math.min(MAX_SIZE, Math.max(1, (int) Math.ceil(width * scale)));
        int mapHeight = Math.min(MAX_SIZE, Math.max(1, (int) Math.ceil(height * scale)));

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] == null) {
                TARGETS[i] = new TextureTarget(MAX_SIZE, MAX_SIZE, false, Minecraft.ON_OSX);
                // Packed integer seed coordinates must never be interpolated.
                TARGETS[i].setFilterMode(GL11.GL_NEAREST);
            }
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_DITHER);
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
        ocular.bindWrite(false);
        RenderSystem.viewport(0, 0, screenWidth, screenHeight);
        RenderSystem.enableScissor(x, y, width, height);
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.setShader(() -> maskShader);
        // Eye-box inversion changes the ocular ID to its complement. Their union is the full ocular,
        // including its texture-alpha silhouette, independently of the current aiming progress.
        RenderSystem.stencilFunc(GL11.GL_EQUAL, stencil, 0xFF);
        drawQuad();
        RenderSystem.stencilFunc(GL11.GL_EQUAL, ~stencil & 0xFF, 0xFF);
        drawQuad();
        RenderSystem.disableScissor();
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        TARGETS[0].bindWrite(false);
        RenderSystem.viewport(0, 0, mapWidth, mapHeight);
        RenderSystem.setShaderTexture(0, ocular.getColorTextureId());
        RenderSystem.setShader(() -> seedShader);
        seedShader.safeGetUniform("SourceSize").set((float) screenWidth, (float) screenHeight);
        seedShader.safeGetUniform("MapSize").set((float) mapWidth, (float) mapHeight);
        seedShader.safeGetUniform("MaskRect").set((float) x, (float) y, (float) width, (float) height);
        drawQuad();

        int source = 0;
        for (int step = Integer.highestOneBit(Math.max(mapWidth, mapHeight) - 1); step > 0; step >>= 1) {
            flood(source, 1 - source, mapWidth, mapHeight, width, height, step);
            source = 1 - source;
        }
        // JFA+1 reduces errors where several edges/corners compete for the nearest seed.
        flood(source, 1 - source, mapWidth, mapHeight, width, height, 1);
        source = 1 - source;
        return new Lookup(TARGETS[source].getColorTextureId(), x, y, width, height, mapWidth, mapHeight);
    }

    private static void flood(int source, int destination, int width, int height,
                              int pixelWidth, int pixelHeight, int step) {
        TARGETS[destination].bindWrite(false);
        RenderSystem.viewport(0, 0, width, height);
        RenderSystem.setShaderTexture(0, TARGETS[source].getColorTextureId());
        RenderSystem.setShader(() -> distanceShader);
        distanceShader.safeGetUniform("MapSize").set((float) width, (float) height);
        distanceShader.safeGetUniform("PixelScale").set((float) pixelWidth / width, (float) pixelHeight / height);
        distanceShader.safeGetUniform("JumpStep").set(step);
        drawQuad();
    }

    private static void drawQuad() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1, -1, 0);
        builder.addVertex(1, -1, 0);
        builder.addVertex(1, 1, 0);
        builder.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    static void close() {
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] != null) TARGETS[i].destroyBuffers();
            TARGETS[i] = null;
        }
    }

    record Lookup(int texture, int x, int y, int width, int height, int mapWidth, int mapHeight) {
    }
}
