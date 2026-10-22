package com.tacz.guns.client.renderer.scope;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Records the existing entity draws; never renders or animates an entity a second time. */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class ScopeThermalRenderer {
    private static final Map<RenderType, BufferBuilder> BUILDERS = new LinkedHashMap<>();
    private static final Map<RenderType, BufferBuilder> ACTIVE = new LinkedHashMap<>();
    private static TextureTarget target;
    private static ShaderInstance shader;
    private static Matrix4f projection;
    private static Matrix4f modelView;
    private static boolean collecting;
    private static boolean ready;

    private ScopeThermalRenderer() {
    }

    static void registerShader(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(GunMod.MOD_ID, "scope_thermal_mask"), DefaultVertexFormat.POSITION_COLOR_TEX),
                    instance -> shader = instance);
        } catch (IOException exception) {
            GunMod.LOGGER.error("Could not load scope thermal mask shader; thermal scopes will use the normal lens", exception);
        }
    }

    static void beginFrame() {
        for (BufferBuilder builder : ACTIVE.values()) builder.end().release();
        ACTIVE.clear();
        projection = null;
        modelView = null;
        collecting = true;
        ready = false;
    }

    private static boolean enabled() {
        Minecraft mc = Minecraft.getInstance();
        return shader != null && RenderConfig.EXPERIMENTAL_SCOPE_LENS.get()
                && RenderConfig.SCOPE_VISION_MODE.get().isThermal()
                && mc.level != null && mc.player != null && mc.getCameraEntity() == mc.player
                && mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui
                && IGun.getIGunOrNull(mc.player.getMainHandItem()) != null
                && IClientPlayerGunOperator.fromLocalPlayer(mc.player).getClientAimingProgress(mc.getFrameTime()) > 0.001f
                && !OculusCompat.isRenderShadow();
    }

    public static MultiBufferSource wrap(MultiBufferSource source, LivingEntity entity) {
        if (!collecting || !enabled() || source instanceof ThermalBuffers || !entity.isAlive()
                || entity.isInvisible() || entity.isSpectator() || entity instanceof ArmorStand) return source;
        return new ThermalBuffers(source);
    }

    @SubscribeEvent
    public static void onWorldStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || OculusCompat.isRenderShadow()) return;
        collecting = false;
        if (!enabled()) return;
        var main = Minecraft.getInstance().getMainRenderTarget();
        try (MemoryStack stack = MemoryStack.stackPush(); var state = new ScopeLensRenderer.RenderState(stack)) {
            // Unsupported render scales/targets fall back to the optical lens for this frame.
            if (state.drawFbo == 0 || state.viewport.get(0) != 0 || state.viewport.get(1) != 0
                    || state.viewport.get(2) != main.width || state.viewport.get(3) != main.height) return;
            int depthType = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            if (depthType != GL11.GL_TEXTURE && depthType != GL30.GL_RENDERBUFFER) return;
            int depthObject = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            int depthLevel = depthType == GL11.GL_TEXTURE
                    ? GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL) : 0;
            RenderSystem.disableScissor();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
            if (target == null) {
                // Own only the mask colour. Borrow the live scene depth with all depth writes disabled.
                target = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
                target.setClearColor(0, 0, 0, 0);
                target.setFilterMode(GL11.GL_LINEAR);
            } else if (target.width != main.width || target.height != main.height) {
                target.resize(main.width, main.height, Minecraft.ON_OSX);
                target.setFilterMode(GL11.GL_LINEAR);
            }
            target.clear(Minecraft.ON_OSX);
            var view = RenderSystem.getModelViewStack();
            view.pushPose();
            target.bindWrite(true);
            try {
                attachSceneDepth(depthType, depthObject, depthLevel);
                // Includes mismatched sample counts/layered attachments: use the optical fallback.
                if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) return;
                if (projection != null) {
                    RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
                    view.setIdentity();
                    view.mulPoseMatrix(modelView);
                    RenderSystem.applyModelViewMatrix();
                }
                var batches = ACTIVE.entrySet().iterator();
                while (batches.hasNext()) {
                    var entry = batches.next();
                    RenderType type = entry.getKey();
                    BufferBuilder.RenderedBuffer vertices = entry.getValue().end();
                    batches.remove();
                    try {
                        type.setupRenderState();
                        target.bindWrite(true);
                        RenderSystem.setShader(() -> shader);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(GL11.GL_LEQUAL);
                        RenderSystem.depthMask(false);
                        RenderSystem.disableBlend();
                        RenderSystem.colorMask(true, true, true, true);
                        BufferBuilder.RenderedBuffer submitted = vertices;
                        vertices = null;
                        BufferUploader.drawWithShader(submitted);
                    } finally {
                        if (vertices != null) vertices.release();
                        type.clearRenderState();
                    }
                }
                ready = true;
            } finally {
                target.bindWrite(false);
                attachSceneDepth(depthType, 0, 0);
                for (BufferBuilder builder : ACTIVE.values()) builder.end().release();
                ACTIVE.clear();
                view.popPose();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }

    private static void attachSceneDepth(int type, int object, int level) {
        if (type == GL11.GL_TEXTURE) {
            GL32.glFramebufferTexture(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, object, level);
        } else {
            GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, object);
        }
    }

    static boolean isReady(int width, int height) {
        return ready && target != null && target.width == width && target.height == height;
    }

    static int textureOr(int fallback, int width, int height) {
        return isReady(width, height) ? target.getColorTextureId() : fallback;
    }

    static void close() {
        beginFrame();
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        // Keep BufferBuilders for reuse: their native allocations are not freed by the GC.
    }

    private record ThermalBuffers(MultiBufferSource source) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            VertexConsumer original = source.getBuffer(type);
            if ((!type.isOutline() && type.outline().isEmpty()) || !collecting) return original;
            if (projection == null) {
                projection = new Matrix4f(RenderSystem.getProjectionMatrix());
                modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            }
            // Keep the material state too: armor's view offset must match the scene depth.
            BufferBuilder builder = ACTIVE.computeIfAbsent(type, key -> {
                BufferBuilder buffer = BUILDERS.computeIfAbsent(key, unused -> new BufferBuilder(4096));
                buffer.begin(key.mode(), DefaultVertexFormat.POSITION_COLOR_TEX);
                return buffer;
            });
            return VertexMultiConsumer.create(original, new MaskVertices(builder));
        }
    }

    /** Equivalent to vanilla's outline generator: keep texture cutouts, ignore lighting and hurt tint. */
    private static final class MaskVertices implements VertexConsumer {
        private final VertexConsumer output;
        private double x, y, z;
        private float u, v;

        MaskVertices(VertexConsumer output) {
            this.output = output;
        }

        @Override public VertexConsumer vertex(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
        @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }

        @Override
        public void endVertex() {
            output.vertex(x, y, z).color(255, 255, 255, 255).uv(u, v).endVertex();
        }
    }
}
