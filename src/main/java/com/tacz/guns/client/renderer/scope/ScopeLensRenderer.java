package com.tacz.guns.client.renderer.scope;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.pojo.display.attachment.ScopeLensConfig;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Local screen-space lens pass, submitted after vanilla hands or Oculus' world composite. */
@OnlyIn(Dist.CLIENT)
public final class ScopeLensRenderer {
    private static final int SEGMENTS = 90;
    private static final float SURFACE_Z = -90;
    private static final List<Capture> CAPTURES = new ArrayList<>();
    private static final List<Lens> LENSES = new ArrayList<>();
    private static ShaderInstance shader;

    private ScopeLensRenderer() {
    }

    public static void registerShader(RegisterShadersEvent event) {
        beginFrame();
        ScopeThermalRenderer.registerShader(event);
        ScopeLensContour.registerShaders(event);
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(GunMod.MOD_ID, "scope_lens"), DefaultVertexFormat.POSITION),
                    instance -> shader = instance);
        } catch (IOException exception) {
            GunMod.LOGGER.error("Could not load experimental scope lens shader; using normal scopes", exception);
        }
    }

    /** Drop unfinished draws too, e.g. after a resize, hidden hands or an interrupted frame. */
    public static void beginFrame() {
        ScopeThermalRenderer.beginFrame();
        for (Lens lens : LENSES) {
            if (lens.surface != null) lens.surface.release();
            for (Draw draw : lens.reticle) {
                if (draw.vertices != null) draw.vertices.release();
            }
        }
        LENSES.clear();
    }

    /** Called with the live scope stencil/pose; callbacks record geometry, without submitting draws. */
    public static boolean capture(float centerX, float centerY, float radius, int stencil, ScopeLensConfig config,
                                  Consumer<VertexConsumer> ocular,
                                  Consumer<MultiBufferSource> reticle) {
        if (shader == null || !RenderConfig.EXPERIMENTAL_SCOPE_LENS.get() || radius <= 0.001f
                || OculusCompat.isRenderShadow()) return false;
        Minecraft mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (drawFbo == 0 || main.width <= 0 || main.height <= 0) return false;
        if (GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) == GL11.GL_NONE) return false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            // The captured mask must use the same pixel grid as the final scene colour.
            if (viewport.get(0) != 0 || viewport.get(1) != 0
                    || viewport.get(2) != main.width || viewport.get(3) != main.height) return false;

            Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            Bounds bounds = new Bounds(new Matrix4f(projection).mul(modelView));
            ocular.accept(bounds);
            if (!bounds.isValid()) return false;

            // Capture does not draw: only framebuffer/scissor and texture-upload state can change.
            int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int activeTexture = GlStateManager._getActiveTexture();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                RenderSystem.disableScissor();
                Capture capture = prepareCapture(main.width, main.height, viewport, stack);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capture.target.frameBufferId);
                // Only the live window mask is needed; capture does not draw or copy depth.
                GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                        GL11.GL_STENCIL_BUFFER_BIT, GL11.GL_NEAREST);
                try {
                    reticle.accept(capture.reticle);
                } catch (RuntimeException exception) {
                    capture.reticle.discard();
                    throw exception;
                }
                LENSES.add(new Lens(capture, stencil, projection, modelView,
                        config, bounds,
                        buildSurface(capture.surface, centerX, centerY, radius), capture.reticle.finish()));
                return true;
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
                // Font baking can upload new glyphs even though the reticle only records vertices.
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                GlStateManager._bindTexture(texture);
                RenderSystem.activeTexture(activeTexture);
                if (scissor) GlStateManager._enableScissorTest(); else RenderSystem.disableScissor();
            }
        }
    }

    /** RenderTarget allocation changes extra state; pay for its snapshot only on creation/resize. */
    private static Capture prepareCapture(int width, int height, IntBuffer viewport, MemoryStack stack) {
        int slot = LENSES.size();
        Capture capture = slot < CAPTURES.size() ? CAPTURES.get(slot) : null;
        if (capture != null && capture.target.frameBufferId >= 0
                && capture.target.width == width && capture.target.height == height) return capture;

        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        FloatBuffer clearColor = stack.mallocFloat(4);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        try {
            if (capture == null) {
                capture = new Capture(width, height);
                CAPTURES.add(capture);
            } else {
                capture.target.resize(width, height, Minecraft.ON_OSX);
            }
            capture.target.setFilterMode(GL11.GL_LINEAR);
            return capture;
        } finally {
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            RenderSystem.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
            RenderSystem.clearDepth(clearDepth);
        }
    }

    /** Both entry points run before screen overlays / Iris' final colour-space conversion. */
    public static void render() {
        if (LENSES.isEmpty()) return;
        var main = Minecraft.getInstance().getMainRenderTarget();
        try (MemoryStack stack = MemoryStack.stackPush(); RenderState state = new RenderState(stack)) {
            var modelView = RenderSystem.getModelViewStack();
            modelView.pushPose();
            try {
                RenderSystem.disableScissor();
                for (Lens lens : LENSES) {
                    TextureTarget target = lens.capture.target;
                    if (target.width != main.width || target.height != main.height || main.getColorTextureId() < 0) continue;
                    ScopeLensContour.Lookup contour = shader != null && RenderConfig.EXPERIMENTAL_SCOPE_LENS.get()
                            && lens.config.hasEdgeEffects()
                            ? ScopeLensContour.prepare(target, lens.stencil, main.width, main.height,
                            lens.centerU - lens.radiusU, lens.centerV - lens.radiusV,
                            lens.centerU + lens.radiusU, lens.centerV + lens.radiusV) : null;
                    target.bindWrite(false);
                    // Pair the saved stencil with the final scene, leaving main's depth/stencil untouched.
                    GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                            GL11.GL_TEXTURE_2D, main.getColorTextureId(), 0);
                    try {
                        RenderSystem.viewport(0, 0, main.width, main.height);
                        RenderSystem.setProjectionMatrix(lens.projection, VertexSorting.DISTANCE_TO_ORIGIN);
                        modelView.setIdentity();
                        modelView.mulPoseMatrix(lens.modelView);
                        RenderSystem.applyModelViewMatrix();
                        GL11.glEnable(GL11.GL_STENCIL_TEST);
                        RenderSystem.stencilFunc(GL11.GL_EQUAL, lens.stencil, 0xFF);
                        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                        RenderSystem.colorMask(true, true, true, true);
                        if (shader != null && RenderConfig.EXPERIMENTAL_SCOPE_LENS.get()) {
                            drawLens(lens, main.width, main.height, contour);
                        }
                        // Reticle and functional readouts retain their original geometry and colours.
                        for (Draw draw : lens.reticle) {
                            draw.type.setupRenderState();
                            try {
                                // A RenderType output state may bind another target (e.g. fabulous graphics).
                                target.bindWrite(false);
                                RenderSystem.disableDepthTest();
                                RenderSystem.depthMask(false);
                                BufferBuilder.RenderedBuffer vertices = draw.vertices;
                                draw.vertices = null; // BufferUploader takes ownership, including empty batches.
                                BufferUploader.drawWithShader(vertices);
                            } finally {
                                draw.type.clearRenderState();
                            }
                        }
                    } finally {
                        target.bindWrite(false);
                        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                GL11.GL_TEXTURE_2D, target.getColorTextureId(), 0);
                    }
                }
            } finally {
                beginFrame();
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }

    private static void drawLens(Lens lens, int width, int height, ScopeLensContour.Lookup contour) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        // Its own colour texture is detached while this FBO writes into main, so reuse it as the snapshot.
        int screenTexture = lens.capture.target.getColorTextureId();
        GlStateManager._bindTexture(screenTexture);
        // Copy first: sampling main's colour texture while drawing back into it is undefined.
        GlStateManager._glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        RenderSystem.setShaderTexture(0, screenTexture);
        // Bind a valid black/scene texture even without a thermal capture; ThermalReady gates sampling.
        RenderSystem.setShaderTexture(1, ScopeThermalRenderer.textureOr(screenTexture, width, height));
        RenderSystem.setShaderTexture(2, contour != null ? contour.texture() : screenTexture);
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("ScreenSize").set((float) width, (float) height);
        shader.safeGetUniform("LensCenter").set(lens.centerU, lens.centerV);
        shader.safeGetUniform("LensRadius").set(lens.radiusU, lens.radiusV);
        shader.safeGetUniform("ContourReady").set(contour != null ? 1 : 0);
        if (contour != null) {
            shader.safeGetUniform("ContourRect").set((float) contour.x(), (float) contour.y(),
                    (float) contour.width(), (float) contour.height());
            shader.safeGetUniform("ContourSize").set((float) contour.mapWidth(), (float) contour.mapHeight());
        }
        ScopeLensConfig config = lens.config;
        shader.safeGetUniform("Distortion").set(config.getDistortion());
        shader.safeGetUniform("Aberration").set(config.getAberration());
        shader.safeGetUniform("EdgeWidth").set(config.getEdgeWidth());
        shader.safeGetUniform("VignetteStrength").set(config.getVignetteStrength());
        shader.safeGetUniform("VignetteWidth").set(config.getVignetteWidth());
        int tint = config.getTint();
        float strength = config.getTintStrength();
        shader.safeGetUniform("LensTint").set(
                1 + (((tint >> 16) & 0xFF) / 255f - 1) * strength,
                1 + (((tint >> 8) & 0xFF) / 255f - 1) * strength,
                1 + ((tint & 0xFF) / 255f - 1) * strength);
        ScopeVisionMode mode = config.getVisionMode();
        if (mode.isThermal() && !RenderConfig.SCOPE_THERMAL_ENABLE.get()) mode = ScopeVisionMode.NORMAL;
        shader.safeGetUniform("VisionMode").set(mode.ordinal());
        shader.safeGetUniform("VisionGain").set(config.getVisionGain());
        shader.safeGetUniform("VisionNoise").set(config.getVisionNoise());
        shader.safeGetUniform("SensorTime").set((net.minecraft.Util.getMillis() % 60000L) / 1000f);
        shader.safeGetUniform("ThermalReady").set(ScopeThermalRenderer.isReady(width, height) ? 1 : 0);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        BufferBuilder.RenderedBuffer surface = lens.surface;
        lens.surface = null;
        BufferUploader.drawWithShader(surface);
    }

    private static BufferBuilder.RenderedBuffer buildSurface(BufferBuilder builder, float x, float y, float radius) {
        // Exactly the same view-space circle used by BedrockAttachmentModel's stencil inversion.
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        builder.vertex(x, y, SURFACE_Z).endVertex();
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i * Math.PI * 2 / SEGMENTS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.vertex(x + cos * radius, y + sin * radius, SURFACE_Z).endVertex();
        }
        return builder.end();
    }

    public static void close() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(ScopeLensRenderer::close);
            return;
        }
        beginFrame();
        ScopeThermalRenderer.close();
        ScopeLensContour.close();
        // Reuse BufferBuilders: their native storage is not released by the GC.
        for (Capture capture : CAPTURES) capture.target.destroyBuffers();
    }

    private static final class Capture {
        final TextureTarget target;
        final BufferBuilder surface = new BufferBuilder(4096);
        final ReticleBuffers reticle = new ReticleBuffers();

        Capture(int width, int height) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.enableStencil();
        }
    }

    private static final class Draw {
        final RenderType type;
        BufferBuilder.RenderedBuffer vertices;

        Draw(RenderType type, BufferBuilder.RenderedBuffer vertices) {
            this.type = type;
            this.vertices = vertices;
        }
    }

    private static final class ReticleBuffers implements MultiBufferSource {
        final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        final Map<RenderType, BufferBuilder> active = new LinkedHashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return active.computeIfAbsent(type, key -> {
                BufferBuilder builder = builders.computeIfAbsent(key, value -> new BufferBuilder(value.bufferSize()));
                builder.begin(key.mode(), key.format());
                return builder;
            });
        }

        List<Draw> finish() {
            List<Draw> draws = new ArrayList<>();
            active.forEach((type, builder) -> draws.add(new Draw(type, builder.end())));
            active.clear();
            return draws;
        }

        void discard() {
            for (BufferBuilder builder : active.values()) builder.end().release();
            active.clear();
        }
    }

    private static final class Lens {
        final Capture capture;
        final ScopeLensConfig config;
        final int stencil;
        final Matrix4f projection;
        final Matrix4f modelView;
        final float centerU;
        final float centerV;
        final float radiusU;
        final float radiusV;
        BufferBuilder.RenderedBuffer surface;
        final List<Draw> reticle;

        Lens(Capture capture, int stencil, Matrix4f projection, Matrix4f modelView,
             ScopeLensConfig config, Bounds bounds, BufferBuilder.RenderedBuffer surface, List<Draw> reticle) {
            this.capture = capture;
            this.config = config;
            this.stencil = stencil;
            this.projection = projection;
            this.modelView = modelView;
            this.centerU = (bounds.minU + bounds.maxU) * 0.5f;
            this.centerV = (bounds.minV + bounds.maxV) * 0.5f;
            this.radiusU = (bounds.maxU - bounds.minU) * 0.5f;
            this.radiusV = (bounds.maxV - bounds.minV) * 0.5f;
            this.surface = surface;
            this.reticle = reticle;
        }
    }

    /** Project the actual ocular geometry, independently of the much larger eye-box circle. */
    private static final class Bounds implements VertexConsumer {
        final Matrix4f projection;
        final Vector4f point = new Vector4f();
        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        boolean behindCamera;

        Bounds(Matrix4f projection) {
            this.projection = projection;
        }

        boolean isValid() {
            return !behindCamera && Float.isFinite(minU) && Float.isFinite(minV)
                    && Float.isFinite(maxU) && Float.isFinite(maxV)
                    && maxU - minU > 0.00001f && maxV - minV > 0.00001f;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            point.set((float) x, (float) y, (float) z, 1).mul(projection);
            if (point.w <= 0.0001f || !Float.isFinite(point.w)) {
                behindCamera = true;
                return this;
            }
            float u = point.x / point.w * 0.5f + 0.5f;
            float v = point.y / point.w * 0.5f + 0.5f;
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
            return this;
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }

    /** Scope drawing must not leak its framebuffer, projection or GL state into later renderers. */
    static final class RenderState implements AutoCloseable {
        final int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        final int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        final IntBuffer viewport;
        final IntBuffer scissorBox;
        final ByteBuffer colorMask;
        final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        final VertexSorting sorting = RenderSystem.getVertexSorting();
        final ShaderInstance previousShader = RenderSystem.getShader();
        final int activeTexture = GlStateManager._getActiveTexture();
        final int[] textures = new int[3];
        final int[] shaderTextures = new int[12];
        final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        final FloatBuffer clearColor;
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean dither = GL11.glIsEnabled(GL11.GL_DITHER);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        final boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final int blendSrc = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        final int blendDst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        final int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        final int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
        final int stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        final int stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
        final int stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
        final int stencilDepthFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
        final int stencilPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);

        RenderState(MemoryStack stack) {
            viewport = stack.mallocInt(4);
            scissorBox = stack.mallocInt(4);
            colorMask = stack.malloc(4);
            clearColor = stack.mallocFloat(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            for (int i = 0; i < shaderTextures.length; i++) shaderTextures[i] = RenderSystem.getShaderTexture(i);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            RenderSystem.activeTexture(activeTexture);
        }

        @Override
        public void close() {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
            BufferUploader.invalidate();
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShader(() -> previousShader);
            for (int i = 0; i < shaderTextures.length; i++) RenderSystem.setShaderTexture(i, shaderTextures[i]);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                GlStateManager._bindTexture(textures[i]);
            }
            RenderSystem.activeTexture(activeTexture);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.clearDepth(clearDepth);
            RenderSystem.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
            RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (dither) GL11.glEnable(GL11.GL_DITHER); else GL11.glDisable(GL11.GL_DITHER);
            RenderSystem.enableScissor(scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3));
            if (!scissor) RenderSystem.disableScissor();
            RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilValueMask);
            RenderSystem.stencilOp(stencilFail, stencilDepthFail, stencilPass);
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST); else GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }
}
