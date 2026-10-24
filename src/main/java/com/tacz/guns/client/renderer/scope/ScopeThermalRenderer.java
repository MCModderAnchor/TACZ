package com.tacz.guns.client.renderer.scope;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.util.ClientRegistryAccess;
import com.tacz.guns.compat.iris.IrisCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.ThermalImagingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Records the existing entity draws; never renders or animates an entity a second time. */
@EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ScopeThermalRenderer {
    // One pixel of depth slope covers subpixel TAA jitter; units cover depth rounding.
    static final float DEPTH_BIAS_FACTOR = -1.0f;
    static final float DEPTH_BIAS_UNITS = -2.0f;
    private static final Map<RenderType, ByteBufferBuilder> BUILDERS = new LinkedHashMap<>();
    private static final Map<RenderType, BufferBuilder> ACTIVE = new LinkedHashMap<>();
    private static TextureTarget target;
    private static ShaderInstance shader;
    private static Matrix4f projection;
    private static Matrix4f modelView;
    private static boolean collecting;
    private static boolean ready;
    private static Boolean thermalScope;

    private ScopeThermalRenderer() {
    }

    public static void registerShader(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope_thermal_mask"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    instance -> shader = instance);
        } catch (IOException exception) {
            GunMod.LOGGER.error("Could not load scope thermal mask shader; thermal scopes will use the normal lens", exception);
        }
    }

    public static void beginFrame() {
        discardActive();
        projection = null;
        modelView = null;
        collecting = true;
        ready = false;
        thermalScope = null;
    }

    public static boolean enabled() {
        Minecraft mc = Minecraft.getInstance();
        if (shader == null || !RenderConfig.EXPERIMENTAL_SCOPE_LENS.get() || !RenderConfig.SCOPE_THERMAL_ENABLE.get()
                || mc.level == null || mc.player == null || mc.getCameraEntity() != mc.player
                || !mc.options.getCameraType().isFirstPerson() || mc.options.hideGui
                || IClientPlayerGunOperator.fromLocalPlayer(mc.player).getClientAimingProgress(mc.getTimer().getGameTimeDeltaPartialTick(true)) <= 0.001f
                || IrisCompat.isRenderShadow()) return false;
        // Resolve attachment NBT and variants once per frame, rather than once per entity draw.
        if (thermalScope == null) thermalScope = hasThermalScope(mc.player.getMainHandItem());
        return thermalScope;
    }

    private static boolean hasThermalScope(ItemStack gunItem) {
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null) return false;
        ItemStack scope = gun.getAttachment(ClientRegistryAccess.registries(), gunItem, AttachmentType.SCOPE);
        if (scope.isEmpty()) scope = gun.getBuiltinAttachment(gunItem, AttachmentType.SCOPE);
        IAttachment attachment = IAttachment.getIAttachmentOrNull(scope);
        if (attachment == null) return false;
        var index = TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(scope)).orElse(null);
        if (index == null) return false;
        var variant = TimelessAPI.getClientAttachmentVariantIndex(scope).orElse(null);
        // Match AttachmentRender's replacement/overlay selection, including unavailable model fallback.
        if (variant != null && variant.getAttachmentModel() != null && variant.getModelTexture() != null) {
            boolean variantThermal = variant.isScope() && variant.getScopeLens().getVisionMode().isThermal();
            if (variant.isReplaceMode()) return variantThermal;
            if (variantThermal) return true;
        }
        return index.isScope() && index.getScopeLens().getVisionMode().isThermal();
    }

    public static MultiBufferSource wrap(MultiBufferSource source, Entity entity) {
        // Nested entity renders evaluate their own tags instead of inheriting the parent's heat mask.
        // Unwrap only our source; other mods' buffer wrappers remain in the chain.
        while (source instanceof ThermalBuffers thermal) source = thermal.source();
        if (!collecting || !enabled() || !entity.isAlive()
                || entity.isInvisible() || entity.isSpectator() || entity == Minecraft.getInstance().getCameraEntity()
                || !ThermalImagingHelper.isHeatSource(entity)) return source;
        return new ThermalBuffers(source);
    }

    @SubscribeEvent
    public static void onWorldStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || IrisCompat.isRenderShadow()) return;
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
            view.pushMatrix();
            target.bindWrite(true);
            try {
                attachSceneDepth(depthType, depthObject, depthLevel);
                // Includes mismatched sample counts/layered attachments: use the optical fallback.
                if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) return;
                if (projection != null) {
                    RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
                    view.identity();
                    view.mul(modelView);
                    RenderSystem.applyModelViewMatrix();
                }
                var batches = ACTIVE.entrySet().iterator();
                while (batches.hasNext()) {
                    var entry = batches.next();
                    RenderType type = entry.getKey();
                    MeshData vertices = entry.getValue().build();
                    batches.remove();
                    if (vertices == null) continue;
                    try {
                        type.setupRenderState();
                        target.bindWrite(true);
                        RenderSystem.setShader(() -> shader);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(GL11.GL_LEQUAL);
                        RenderSystem.depthMask(false);
                        // Bias only mask depth: keep cutout silhouettes and scene occlusion intact.
                        // Apply after the material state, which may set its own polygon offset.
                        RenderSystem.enablePolygonOffset();
                        RenderSystem.polygonOffset(DEPTH_BIAS_FACTOR, DEPTH_BIAS_UNITS);
                        RenderSystem.disableBlend();
                        RenderSystem.colorMask(true, true, true, true);
                        MeshData submitted = vertices;
                        vertices = null;
                        BufferUploader.drawWithShader(submitted);
                    } finally {
                        if (vertices != null) vertices.close();
                        type.clearRenderState();
                    }
                }
                ready = true;
            } finally {
                target.bindWrite(false);
                attachSceneDepth(depthType, 0, 0);
                discardActive();
                view.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }

    public static void attachSceneDepth(int type, int object, int level) {
        if (type == GL11.GL_TEXTURE) {
            GL32.glFramebufferTexture(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, object, level);
        } else {
            GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, object);
        }
    }

    public static boolean isReady(int width, int height) {
        return ready && target != null && target.width == width && target.height == height;
    }

    public static int textureOr(int fallback, int width, int height) {
        return isReady(width, height) ? target.getColorTextureId() : fallback;
    }

    public static void close() {
        beginFrame();
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        BUILDERS.values().forEach(ByteBufferBuilder::close);
        BUILDERS.clear();
    }

    private static void discardActive() {
        for (BufferBuilder builder : ACTIVE.values()) {
            MeshData vertices = builder.build();
            if (vertices != null) vertices.close();
        }
        ACTIVE.clear();
    }

    public record ThermalBuffers(MultiBufferSource source) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            VertexConsumer original = source.getBuffer(type);
            if ((!type.isOutline() && type.outline().isEmpty()) || !collecting) return original;
            if (projection == null) {
                projection = new Matrix4f(RenderSystem.getProjectionMatrix());
                modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            }
            // Keep the material state too: armor's view offset must match the scene depth.
            BufferBuilder builder = ACTIVE.computeIfAbsent(type, key -> new BufferBuilder(
                    BUILDERS.computeIfAbsent(key, unused -> new ByteBufferBuilder(4096)),
                    key.mode(), DefaultVertexFormat.POSITION_TEX_COLOR));
            return VertexMultiConsumer.create(original, new MaskVertices(builder));
        }
    }

    /** Equivalent to vanilla's outline generator: keep texture cutouts, ignore lighting and hurt tint. */
    public static final class MaskVertices implements VertexConsumer {
        private final VertexConsumer output;

        MaskVertices(VertexConsumer output) {
            this.output = output;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            output.addVertex(x, y, z).setColor(255, 255, 255, 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            output.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

    }
}
