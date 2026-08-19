package com.tacz.guns.client.particle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * 第一人称枪口粒子管理器：将 SBM 2.5.1 的 snowstorm 粒子系统接入枪械开火流程。
 * <p>
 * 触发链路：
 * <ul>
 *   <li>开枪（{@code GunFireEvent}）→ {@link #onShoot} 解析枪械 display / 配件参数并入队；</li>
 *   <li>动画 {@code particle_effects} 关键帧 → 渲染时经
 *       {@link #spawnAndBind} 的 {@code animationParticles} 参数入队；</li>
 *   <li>每帧第一人称渲染时 {@link #spawnAndBind} 把发射器变换绑定到
 *       {@code muzzle_flash} 骨骼（含配件偏移与缩放），新发射器立即 {@code tick(0)} 触发瞬时发射。</li>
 * </ul>
 * 粒子系统的 tick / 渲染由 SBM 的 {@code FirstPersonRenderHandler} 统一接管，本类只负责创建与绑定。
 */
public final class MuzzleParticleManager {
    /** 开枪触发的待创建请求（按手）。 */
    private static final EnumMap<InteractionHand, List<PendingShoot>> PENDING = new EnumMap<>(InteractionHand.class);
    /** 已创建发射器 → 定位骨骼名（按手）。 */
    private static final EnumMap<InteractionHand, List<BoundEmitter>> BOUND = new EnumMap<>(InteractionHand.class);
    /** 基准 pose（由渲染器在粒子渲染 pass 捕获，按手）。 */
    private static final EnumMap<InteractionHand, Matrix4f> BASE_POSE = new EnumMap<>(InteractionHand.class);

    private MuzzleParticleManager() {
    }

    // ==================== 公开入口 ====================

    /**
     * 解析一次开火的枪口粒子参数（第一人称与第三人称共用）。
     * 返回 null 表示无需粒子（非 renew 枪 / 消音器隐藏整组 / 无可用预设）。
     */
    @Nullable
    public static MuzzleShot resolveShot(ItemStack gunItem) {
        GunDisplayInstance display = TimelessAPI.getGunDisplay(gunItem).orElse(null);
        if (display == null) {
            return null;
        }
        MuzzleFlash muzzleFlash = display.getMuzzleFlash();
        if (muzzleFlash == null || !muzzleFlash.isRenew()) {
            return null;
        }
        MuzzleAttachmentEffect effect = resolveAttachmentEffect(gunItem);

        ResourceLocation flashId = muzzleFlash.getTextureSet();
        if (flashId == null) {
            flashId = MuzzleParticlePresets.forGunType(gunTypeOf(gunItem));
        }
        if (flashId == null) {
            return null;
        }

        float gunScale = muzzleFlash.getScale();
        // 未配置 flash_hider 的消音器：隐藏枪焰（flashScale=0），保留枪烟。
        float flashScale = gunScale * effect.flashScale();
        if (effect.silenced() && !effect.hasFlashHider()) {
            flashScale = 0.0f;
        }
        return new MuzzleShot(flashId,
                flashScale,
                MuzzleParticlePresets.smokeOf(flashId),
                gunScale * effect.smokeScale(),
                effect.offsetBb());
    }

    /**
     * 客户端开火事件调用（仅本机玩家主手，与旧版枪口火光一致）。
     */
    public static void onShoot(ItemStack gunItem) {
        // 第一人称粒子仅在第一人称视角下有持枪渲染层消费；第三人称视角下
        // 本机不渲染第一人称持枪，入队只会堆积、切回第一人称时一次性全发。
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            enqueueShoot(gunItem);
        }
    }

    /**
     * 将一次开火请求入队，供第一人称渲染时消费。供按视角门控后的内部使用。
     */
    public static void enqueueShoot(ItemStack gunItem) {
        MuzzleShot shot = resolveShot(gunItem);
        if (shot == null) {
            return;
        }
        PENDING.computeIfAbsent(InteractionHand.MAIN_HAND, h -> new ArrayList<>()).add(new PendingShoot(
                shot.flashId(), shot.flashScale(), shot.smokeId(), shot.smokeScale(), shot.offsetBb()));
    }

    /**
     * 渲染器在粒子渲染 pass（渲染枪体前）捕获基准矩阵，按手保存。
     */
    public static void captureBasePose(InteractionHand hand, Matrix4f pose) {
        BASE_POSE.put(hand, new Matrix4f(pose));
    }

    /**
     * 获取该手的基准矩阵（渲染枪体前捕获的入口 poseStack）。
     * 粒子渲染时需把 poseStack 设回此矩阵，保证渲染基准与发射器绑定时一致。
     */
    @Nullable
    public static Matrix4f getBasePose(InteractionHand hand) {
        Matrix4f pose = BASE_POSE.get(hand);
        return pose != null ? new Matrix4f(pose) : null;
    }

    /**
     * 第一人称渲染枪体后调用：消费待创建请求与动画粒子请求，创建发射器并绑定到枪口骨骼。
     *
     * @param system             SBM 全局第一人称粒子系统
     * @param poseStack          已应用完整第一人称定位变换的 poseStack（枪体视图空间）
     * @param model              当前枪械模型
     * @param hand               渲染所在手
     * @param animationParticles 本帧动画 {@code particle_effects} 通道触发的粒子请求（可为 null）
     */
    public static void spawnAndBind(FirstPersonParticleSystem system, PoseStack poseStack,
                                    BedrockGunModel model, InteractionHand hand,
                                    @Nullable List<ParticleEffectData> animationParticles) {
        Matrix4f basePose = BASE_POSE.get(hand);
        if (basePose == null) {
            return;
        }
        // 没有枪口定位骨骼的枪械无法绑定粒子，直接丢弃本帧请求
        if (model.getMuzzleFlashPosPath() == null) {
            PENDING.remove(hand);
            return;
        }
        Matrix4f basePoseInv = new Matrix4f(basePose).invert();

        List<BoundEmitter> bound = BOUND.computeIfAbsent(hand, h -> new ArrayList<>());
        List<ParticleEmitterInstance> newEmitters = new ArrayList<>();

        // 1. 消费开枪触发的待创建请求。
        List<PendingShoot> pending = PENDING.remove(hand);
        if (pending != null) {
            for (PendingShoot req : pending) {
                // scale <= 0 表示隐藏该层（如 flash_hider: 0），直接跳过
                if (req.flashScale() > 0) {
                    createEmitter(system, hand, bound, newEmitters, req.flashId(), req.flashScale(), "muzzle_flash", req.offsetBb());
                }
                if (RenderConfig.RENDER_MUZZLE_SMOKE.get() && req.smokeScale() > 0 && req.smokeId() != null) {
                    createEmitter(system, hand, bound, newEmitters, req.smokeId(), req.smokeScale(), "muzzle_flash", req.offsetBb());
                }
            }
        }
        // 2. 消费动画 particle_effects 关键帧请求。
        if (animationParticles != null) {
            for (ParticleEffectData data : animationParticles) {
                String locator = data.locator() == null || data.locator().isEmpty() ? "muzzle_flash" : data.locator();
                createEmitter(system, hand, bound, newEmitters, data.effect(), 1.0f, locator, 0.0f);
            }
        }

        // 3. 计算枪口骨骼在视图空间中的矩阵（含当前动画变换）。
        poseStack.pushPose();
        List<BedrockPart> muzzlePath = model.getMuzzleFlashPosPath();
        if (muzzlePath != null) {
            for (BedrockPart part : muzzlePath) {
                part.translateAndRotateAndScale(poseStack);
            }
        }
        Matrix4f muzzleBone = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float cameraRoll = CameraStateCache.getCameraRollRadians();
        Matrix4f cameraRotationInv = new Matrix4f()
                .rotationX((float) Math.toRadians(camera.getXRot()))
                .rotateY((float) Math.toRadians(camera.getYRot() + 180f))
                .rotateZ(cameraRoll)
                .invert();

        // 4. 为本手所有存活发射器刷新变换。
        for (BoundEmitter be : bound) {
            ParticleEmitterInstance emitter = be.emitter();
            if (emitter.isFinished()) {
                continue;
            }
            Matrix4f muzzleView = new Matrix4f(muzzleBone);
            // 沿枪口方向前移（Blockbench 单位 / 16 = 格）
            if (be.offsetBb() != 0.0f) {
                muzzleView.translate(0.0f, 0.0f, -be.offsetBb() / 16.0f);
            }
            // 尺寸/强度参数经每实例 molang 变量（v.muzzle_scale）传递，不在此缩放矩阵
            emitter.setEmitterTransform(new Matrix4f(basePoseInv).mul(muzzleView),
                    new Matrix4f(cameraRotationInv).mul(muzzleView));
        }

        // 5. 新发射器立即 tick(0) 触发 instant 发射，本帧即可见。
        for (ParticleEmitterInstance emitter : newEmitters) {
            emitter.tick(0f);
        }

        // 6. 清理已自然消亡的发射器引用。
        bound.removeIf(be -> be.emitter().isFinished());
    }

    // ==================== 内部实现 ====================

    private static void createEmitter(FirstPersonParticleSystem system, InteractionHand hand, List<BoundEmitter> bound,
                                      List<ParticleEmitterInstance> newEmitters, ResourceLocation definitionId,
                                      float scale, String locator, float offsetBb) {
        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(definitionId);
        if (definition == null) {
            GunMod.LOGGER.warn("Muzzle particle definition {} not found, skipped", definitionId);
            return;
        }
        ParticleEmitterInstance emitter = system.addEmitter(definition, hand);
        // 通过每实例 molang 变量把尺寸/强度参数传给定义（v.muzzle_scale）
        emitter.setVariable("muzzle_scale", scale);
        bound.add(new BoundEmitter(emitter, locator, offsetBb));
        newEmitters.add(emitter);
    }

    @Nullable
    private static String gunTypeOf(ItemStack gunItem) {
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null) {
            return null;
        }
        return TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem))
                .map(com.tacz.guns.resource.index.CommonGunIndex::getType)
                .orElse(null);
    }

    private static MuzzleAttachmentEffect resolveAttachmentEffect(ItemStack gunItem) {
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null) {
            return MuzzleAttachmentEffect.NONE;
        }
        ItemStack muzzleAttachment = gun.getAttachment(gunItem, AttachmentType.MUZZLE);
        if (muzzleAttachment.isEmpty()) {
            muzzleAttachment = gun.getBuiltinAttachment(gunItem, AttachmentType.MUZZLE);
        }
        IAttachment attachment = IAttachment.getIAttachmentOrNull(muzzleAttachment);
        if (attachment == null) {
            return MuzzleAttachmentEffect.NONE;
        }
        ResourceLocation attachmentId = attachment.getAttachmentId(muzzleAttachment);
        float offsetBb = TimelessAPI.getClientAttachmentIndex(attachmentId)
                .map(com.tacz.guns.client.resource.index.ClientAttachmentIndex::getMuzzleFlashOffset)
                .orElse(0.0f);
        AttachmentData data = TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem))
                .map(index -> index.getGunData().getExclusiveAttachments().get(attachmentId))
                .orElse(null);
        if (data == null) {
            data = TimelessAPI.getCommonAttachmentIndex(attachmentId)
                    .map(com.tacz.guns.resource.index.CommonAttachmentIndex::getData)
                    .orElse(null);
        }
        boolean hasFlashHider = data != null && data.hasFlashHider();
        float flashScale = hasFlashHider ? data.getFlashHider() : 1.0f;
        float smokeScale = data != null ? data.getSmokeHider() : 1.0f;
        return new MuzzleAttachmentEffect(offsetBb, flashScale, smokeScale, isSilenced(data), hasFlashHider);
    }

    private static boolean isSilenced(@Nullable AttachmentData data) {
        if (data == null) {
            return false;
        }
        var silence = data.getModifier().get(SilenceModifier.ID);
        return silence != null && silence.getValue() instanceof Pair<?, ?> pair
                && pair.right() instanceof Boolean value && value;
    }

    // ==================== 数据结构 ====================

    /**
     * 一次开火的枪口粒子参数（第一人称/第三人称共用）。
     */
    public record MuzzleShot(ResourceLocation flashId, float flashScale,
                             @Nullable ResourceLocation smokeId, float smokeScale, float offsetBb) {
    }

    private record PendingShoot(ResourceLocation flashId, float flashScale,
                                @Nullable ResourceLocation smokeId, float smokeScale, float offsetBb) {
    }

    private record BoundEmitter(ParticleEmitterInstance emitter, String locator, float offsetBb) {
    }

    private record MuzzleAttachmentEffect(float offsetBb, float flashScale, float smokeScale,
                                          boolean silenced, boolean hasFlashHider) {
        private static final MuzzleAttachmentEffect NONE =
                new MuzzleAttachmentEffect(0.0f, 1.0f, 1.0f, false, false);
    }
}
