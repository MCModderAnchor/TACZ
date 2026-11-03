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
import com.tacz.guns.resource.pojo.data.SmokeDirectionData;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.util.time.GunTime;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
    private static final long FLASH_LIGHT_TIME_MS = 50;
    /** 开枪触发的待创建请求（按手）。 */
    private static final EnumMap<InteractionHand, List<PendingShoot>> PENDING = new EnumMap<>(InteractionHand.class);
    /** 已创建发射器 → 定位骨骼名（按手）。 */
    private static final EnumMap<InteractionHand, List<BoundEmitter>> BOUND = new EnumMap<>(InteractionHand.class);
    /** 基准 pose（由渲染器在粒子渲染 pass 捕获，按手）。 */
    private static final EnumMap<InteractionHand, Matrix4f> BASE_POSE = new EnumMap<>(InteractionHand.class);
    private static GunDisplayInstance lightDisplay;
    private static long lightShootTime = -1;
    private static int flashLightLevel;

    private MuzzleParticleManager() {
    }

    // ==================== 公开入口 ====================

    /**
     * 解析一次开火的枪口粒子参数（第一人称与第三人称共用）。
     * 返回 null 表示无需粒子（非 renew 枪）。
     * <p>
     * 4 个粒子 id（1p 闪光 / 1p 烟雾 / 3p 闪光 / 3p 烟雾）直接取 display 配置：
     * 未配置时字段保留 {@link MuzzleFlash} 中的默认常量；显式写 {@code null} 表示
     * 对应效果不生成（id 为 null），消费端据此跳过创建。
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

        float gunScale = muzzleFlash.getScale();
        // 未配置 flash_hider 的消音器：隐藏枪焰（flashScale=0），保留枪烟。
        float flashScale = gunScale * effect.flashScale();
        if (effect.silenced() && !effect.hasFlashHider()) {
            flashScale = 0.0f;
        }
        int flashLightLevel = Math.max(0, Math.min(15,
                (int) Math.ceil(muzzleFlash.getFlashLightLevel() * effect.flashScale())));
        // 枪烟尺寸与枪焰 scale 完全解耦：未配置时返回 0，由粒子定义使用内置默认大小。
        float smokeSize = applyAttachmentScale(muzzleFlash.getSmokeScale(), BUILTIN_SMOKE_SIZE, effect.smokeScale());
        return new MuzzleShot(muzzleFlash.getFirstPersonFlashId(),
                flashScale,
                flashLightLevel,
                muzzleFlash.getFirstPersonSmokeId(),
                gunScale * effect.smokeScale(),
                effect.offsetBb(),
                muzzleFlash.getThirdPersonFlashId(),
                muzzleFlash.getThirdPersonSmokeId(),
                // 速度与数量按配件倍率相乘：枪械给绝对值，配件只做相对调整。
                muzzleFlash.getInitialSpeedScale() * effect.smokeSpeedScale(),
                applyAttachmentScale(muzzleFlash.getSmokeParticleCount(), BUILTIN_SMOKE_PARTICLE_COUNT, effect.smokeCountScale()),
                smokeSize,
                resolveSmokeDirection(muzzleFlash, effect));
    }

    /**
     * 解析最终的枪烟发射方向。
     * <p>
     * 方向与倍率类字段的取舍规则不同：<b>枪口配件优先</b>。配件本身就替换了枪口模型，
     * 排烟方式随之接管；配件没配（或压根没装配件）才回退到枪械 display 的默认值，
     * 这样裸枪也能有自己的排烟表现。
     */
    @Nullable
    private static SmokeDirection resolveSmokeDirection(MuzzleFlash muzzleFlash, MuzzleAttachmentEffect effect) {
        SmokeDirectionData.Config config = effect.smokeDirectionConfig() != null
                ? effect.smokeDirectionConfig() : muzzleFlash.getSmokeDirectionConfig();
        if (config == null) {
            return null;
        }
        float[][] raw = config.directions();
        // 粒子的局部空间取自枪体的 poseStack，而渲染前对它做过一次绕 Z 轴 180° 的翻转
        // （1p 见 GunItemRendererWrapper#renderFirstPerson 的 rotateZ(180°)，
        //  3p 见 GunItemTransforms 的 scale(-1, -1, 1)，两者是同一个变换 (x,y,z) → (-x,-y,z)），
        // 所以那个空间实际是 +X 左、+Y 下。不翻回来的话，枪包作者按直觉写的"上（+Y）"
        // 会得到朝下的烟。这里抵消掉翻转，让配置空间回到直觉：+X 右、+Y 上、-Z 枪口前方。
        // Z 不受影响，与 muzzle_flash_offset 的前移方向（同为 -Z）保持同一套轴向。
        float[][] fixed = new float[raw.length][];
        for (int i = 0; i < raw.length; i++) {
            fixed[i] = new float[]{-raw[i][0], -raw[i][1], raw[i][2]};
        }
        return new SmokeDirection(fixed, config.spread(), config.angleMode());
    }

    /**
     * 枪烟粒子数量的内置默认值，须与 {@code muzzle_flash_smoke.particle.json} 中
     * {@code v.smoke_count == 0 ? 15 : v.smoke_count} 的兜底值保持一致。
     */
    private static final float BUILTIN_SMOKE_PARTICLE_COUNT = 15.0f;

    /**
     * 枪烟尺寸的内置默认值。粒子定义里 {@code v.smoke_size} 为 0 与非 0 走的是同一套
     * 尺寸曲线，只是 0 时代入常量 1.0，故这里取 1.0 等价。
     */
    private static final float BUILTIN_SMOKE_SIZE = 1.0f;

    /**
     * 把配件倍率作用到枪械 display 的枪烟参数上。
     * <p>
     * 这些字段用 <b>0 作为哨兵值</b>表示"未配置，交给粒子定义的内置默认值"。
     * 若直接相乘，{@code 0 × k} 恒为 0，哨兵原样透传到粒子定义、又回退成内置默认值，
     * 配件倍率就被静默吞掉了。因此枪械未配置时改用内置默认值作为基数，
     * 倍率才能真正作用于最终结果。
     * <p>
     * 倍率为 1.0（配件没写这个字段）时原样透传，保证不装配件时的行为与旧版完全一致。
     *
     * @param gunValue       枪械 display 中配置的值，0 表示未配置
     * @param builtinDefault 粒子定义中的内置默认值，仅在枪械未配置时顶替
     * @param scale          配件提供的倍率
     */
    private static float applyAttachmentScale(float gunValue, float builtinDefault, float scale) {
        if (scale == 1.0f) {
            return gunValue;
        }
        return (gunValue > 0.0f ? gunValue : builtinDefault) * scale;
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
            resetMuzzleLight();
            return;
        }
        lightDisplay = TimelessAPI.getGunDisplay(gunItem).orElse(null);
        lightShootTime = GunTime.nowMillis();
        flashLightLevel = shot.flashId() != null && shot.flashScale() > 0 ? shot.flashLightLevel() : 0;
        PENDING.computeIfAbsent(InteractionHand.MAIN_HAND, h -> new ArrayList<>()).add(new PendingShoot(
                shot.flashId(), shot.flashScale(), shot.smokeId(), shot.smokeScale(), shot.offsetBb(), shot.speedScale(), shot.smokeParticleCount(), shot.smokeSize(), shot.smokeDirection()));
    }

    /** 仅提升当前第一人称枪械的方块光，保留环境天空光。 */
    public static int getFirstPersonModelLight(ItemStack gunItem, int environmentLight) {
        if (lightDisplay == null || flashLightLevel <= 0 || !GunTime.isWithin(lightShootTime, FLASH_LIGHT_TIME_MS)
                || TimelessAPI.getGunDisplay(gunItem).orElse(null) != lightDisplay) {
            return environmentLight;
        }
        int environmentBlockLight = LightTexture.block(environmentLight);
        int environmentSkyLight = LightTexture.sky(environmentLight);
        if (environmentBlockLight >= 12) {
            return environmentLight;
        }
        // 暗处最多提高十级，火光提亮上限为十二级；保留更亮的环境光。
        int flashLightLimit = Math.min(12, environmentBlockLight + 10);
        int blockLight = Math.max(environmentBlockLight, Math.min(flashLightLevel, flashLightLimit));
        return LightTexture.pack(blockLight, environmentSkyLight);
    }

    public static void resetMuzzleLight() {
        lightDisplay = null;
        lightShootTime = -1;
        flashLightLevel = 0;
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
                // id 为 null 表示 display 显式配置为不生成该层；scale <= 0 表示隐藏该层（如 flash_hider: 0）
                if (req.flashId() != null && req.flashScale() > 0) {
                    createFlashEmitter(system, hand, bound, newEmitters, req.flashId(),
                            req.flashScale(), "muzzle_flash", req.offsetBb(), req.speedScale());
                }
                if (RenderConfig.RENDER_MUZZLE_SMOKE.get() && req.smokeId() != null && req.smokeScale() > 0) {
                    createSmokeEmitter(system, hand, bound, newEmitters, req.smokeId(),
                            req.smokeScale(), "muzzle_flash", req.offsetBb(), req.speedScale(),
                            req.smokeSize(), req.smokeParticleCount(), req.smokeDirection());
                }
            }
        }
        // 2. 消费动画 particle_effects 关键帧请求。
        if (animationParticles != null) {
            for (ParticleEffectData data : animationParticles) {
                String locator = data.locator() == null || data.locator().isEmpty() ? "muzzle_flash" : data.locator();
                createFlashEmitter(system, hand, bound, newEmitters, data.effect(),
                        1.0f, locator, 0.0f, 1.0f);
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
            // 投递到世界空间的粒子（枪烟等）尺寸按手部 FOV 的观感缩放，与枪体/枪焰保持一致
            emitter.setWorldParticleScale(getWorldParticleScale());
            emitter.setEmitterTransform(new Matrix4f(basePoseInv).mul(muzzleView),
                    new Matrix4f(cameraRotationInv).mul(toWorldMuzzleView(muzzleView)));
        }

        // 5. 新发射器立即 tick(0) 触发 instant 发射，本帧即可见。
        for (ParticleEmitterInstance emitter : newEmitters) {
            emitter.tick(0f);
        }

        // 6. 清理已自然消亡的发射器引用。
        bound.removeIf(be -> be.emitter().isFinished());
    }

    // ==================== 内部实现 ====================

    /**
     * 手部（枪械 model_fov）与世界（玩家 FOV）的正切半径比：tan(modelFov / 2) / tan(worldFov / 2)。
     * 等于 1 表示两者投影一致，无需补偿。
     */
    private static float getHandToWorldTanRatio() {
        return CameraStateCache.getFirstPersonToWorldDepthScale();
    }

    /**
     * 构造"世界对齐"的枪口矩阵（相对摄像机、未叠加相机世界坐标）。
     * <p>
     * 手部 pass 用枪械 {@code model_fov}、世界 pass 用玩家 FOV：同一个世界点在世界 pass 中投到
     * {@code x / (d·tan(worldFov/2))}，在手部 pass 中投到 {@code x / (d·tan(modelFov/2))}。
     * 要让投递到原版 {@code ParticleEngine} 的世界空间粒子落在手部枪口的屏幕位置上，
     * 只需把整个局部空间的横向 (x, y) 乘 {@code tan(worldFov/2) / tan(modelFov/2)}，深度 z 保持真实：
     * 粒子的世界深度因此不变，尺寸、遮挡、透视都跟随世界 FOV（与第三人称枪烟一致），
     * 不会被旧实现的深度压缩（z 乘 tan(modelFov/2)/tan(worldFov/2)）锁死在模型 FOV 上，
     * 也不会被搬到更近/更远的深度上；整段局部空间一起缩放还保证云团的横向铺开在屏幕上也与
     * 手部 FOV 下的观感一致（发射形状偏移、跟随锚点的局部偏移同一套映射）。
     * <p>
     * 注意：这里连矩阵基一起缩放。SBM 侧从定位器变换读取出生尺寸，并在世界粒子
     * 绘制和后续加速度更新时处理 FOV 比例，避免旧粒子尺寸混杂或粒子群散布不一致。
     */
    private static Matrix4f toWorldMuzzleView(Matrix4f muzzleView) {
        float fovRatio = getHandToWorldTanRatio();
        if (fovRatio <= 1.0e-4f || Math.abs(fovRatio - 1.0f) <= 1.0e-4f) {
            return new Matrix4f(muzzleView);
        }
        float lateral = 1.0f / fovRatio;
        return new Matrix4f().scaling(lateral, lateral, 1.0f).mul(muzzleView);
    }

    /**
     * 世界空间粒子（投递到原版 {@code ParticleEngine} 的部分，如枪烟）的尺寸/初速度缩放。
     * <p>
     * 这批粒子的屏幕位置由 {@link #toWorldMuzzleView} 补偿到手部 FOV 的枪口、深度保持真实，
     * 但"真实深度 + 世界 FOV"会让它们的屏幕大小随玩家 FOV 变化：世界 FOV 很小时（如 30）
     * 会明显胀大糊满屏幕。而枪体与枪焰都按 {@code model_fov} 绘制，尺寸不随玩家 FOV 变化，
     * 因此这里按 {@code tan(worldFov/2) / tan(modelFov/2)} 缩放，让世界空间粒子在屏幕上
     * 始终保持手部 FOV 下的观感。
     */
    private static float getWorldParticleScale() {
        float ratio = getHandToWorldTanRatio();
        return ratio > 1.0e-4f ? 1.0f / ratio : 1.0f;
    }

    /**
     * 创建枪焰发射器。只写入 {@code muzzle_*} 命名空间的变量。
     * <p>
     * 与枪烟发射器使用互不重叠的变量名：第一人称下所有发射器共用同一个
     * SBM {@code ParticleMolangEnvironment}，变量覆盖按名字索引，同名变量后者会覆盖前者。
     * 历史上枪焰与枪烟都写 {@code muzzle_scale} / {@code muzzle_speed}，导致枪烟粒子
     * 读到的是枪焰最后写入的值（枪焰的 smokeSize 恒为 0），枪烟尺寸被刷成 0 而不可见。
     * 拆分命名空间后两者互不干扰。
     */
    private static void createFlashEmitter(FirstPersonParticleSystem system, InteractionHand hand,
                                           List<BoundEmitter> bound, List<ParticleEmitterInstance> newEmitters,
                                           ResourceLocation definitionId, float scale, String locator,
                                           float offsetBb, float speedScale) {
        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(definitionId);
        if (definition == null) {
            GunMod.LOGGER.warn("Muzzle particle definition {} not found, skipped", definitionId);
            return;
        }
        ParticleEmitterInstance emitter = system.addEmitter(definition, hand);
        // 枪焰尺寸 → v.muzzle_scale
        emitter.setVariable("muzzle_scale", scale);
        // 枪焰初速度倍率 → v.muzzle_speed
        emitter.setVariable("muzzle_speed", speedScale);
        bound.add(new BoundEmitter(emitter, locator, offsetBb));
        newEmitters.add(emitter);
    }

    /**
     * 创建枪烟发射器。只写入 {@code smoke_*} 命名空间的变量，与枪焰完全隔离。
     *
     * @param smokeSize         枪烟尺寸倍率（0 表示使用粒子定义内置默认大小）
     * @param smokeParticleCount 枪烟粒子数量（0 表示使用粒子定义内置默认值）
     * @param smokeDirection    配件指定的发射方向；null 表示不干预，粒子定义回退到均匀四散
     */
    private static void createSmokeEmitter(FirstPersonParticleSystem system, InteractionHand hand,
                                           List<BoundEmitter> bound, List<ParticleEmitterInstance> newEmitters,
                                           ResourceLocation definitionId, float scale, String locator,
                                           float offsetBb, float speedScale, float smokeSize,
                                           float smokeParticleCount,
                                           @Nullable SmokeDirection smokeDirection) {
        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(definitionId);
        if (definition == null) {
            GunMod.LOGGER.warn("Muzzle particle definition {} not found, skipped", definitionId);
            return;
        }
        ParticleEmitterInstance emitter = system.addEmitter(definition, hand);
        // 枪烟尺寸倍率 → v.smoke_size
        emitter.setVariable("smoke_size", smokeSize);
        // 枪烟初速度倍率 → v.smoke_vel
        emitter.setVariable("smoke_vel", speedScale);
        // 枪烟粒子数量 → v.smoke_count
        emitter.setVariable("smoke_count", smokeParticleCount);
        writeSmokeDirectionVariables(emitter, smokeDirection);
        bound.add(new BoundEmitter(emitter, locator, offsetBb));
        newEmitters.add(emitter);
    }

    /**
     * 写入枪烟发射方向相关的 molang 变量。
     * <p>
     * 方向列表平铺成固定 {@link SmokeDirectionData#MAX_DIRECTIONS} 组标量变量
     * （{@code v.smoke_dir<i>_x / _y / _z}），配合方向数量 {@code v.smoke_dir_n}
     * 供粒子定义按 {@code v.particle_random_1} 挑选。未用到的槽位一律写 0。
     * <p>
     * 第一、第三人称都调用本方法，把未使用的变量显式写成 0：
     * 粒子定义里的方向是乘积形式，若某个分量未定义且默认值为 NaN，{@code 0 * NaN}
     * 仍是 NaN，不会归零。另外变量存储在同帧内是所有发射器共享的，写全也避免读到
     * 其他发射器的残留值。
     */
    static void writeSmokeDirectionVariables(ParticleEmitterInstance emitter, @Nullable SmokeDirection direction) {
        int count = direction == null ? 0 : direction.dirs().length;
        emitter.setVariable("smoke_dir_n", count);
        emitter.setVariable("smoke_spread", direction == null ? 0.0f : direction.spread());
        emitter.setVariable("smoke_angle_mode", direction != null && direction.angleMode() ? 1.0f : 0.0f);
        for (int i = 0; i < SmokeDirectionData.MAX_DIRECTIONS; i++) {
            float x = 0.0f;
            float y = 0.0f;
            float z = 0.0f;
            float ux = 0.0f;
            float uy = 0.0f;
            float uz = 0.0f;
            float vx = 0.0f;
            float vy = 0.0f;
            float vz = 0.0f;
            if (direction != null && i < count) {
                float[] dir = direction.dirs()[i];
                x = dir[0];
                y = dir[1];
                z = dir[2];
                float lateral = (float) Math.hypot(x, y);
                if (lateral > 1.0e-6f) {
                    ux = -y / lateral;
                    uy = x / lateral;
                } else {
                    ux = 1.0f;
                }
                vx = y * uz - z * uy;
                vy = z * ux - x * uz;
                vz = x * uy - y * ux;
            }
            emitter.setVariable("smoke_dir" + i + "_x", x);
            emitter.setVariable("smoke_dir" + i + "_y", y);
            emitter.setVariable("smoke_dir" + i + "_z", z);
            emitter.setVariable("smoke_u" + i + "_x", ux);
            emitter.setVariable("smoke_u" + i + "_y", uy);
            emitter.setVariable("smoke_u" + i + "_z", uz);
            emitter.setVariable("smoke_v" + i + "_x", vx);
            emitter.setVariable("smoke_v" + i + "_y", vy);
            emitter.setVariable("smoke_v" + i + "_z", vz);
        }
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
        // 速度与数量同样按倍率处理：配件不覆盖枪械的绝对值，只在其基础上做相对调整。
        float smokeSpeedScale = data != null ? data.getSmokeSpeedScale() : 1.0f;
        float smokeCountScale = data != null ? data.getSmokeCountScale() : 1.0f;
        // 方向是例外：方向没有天然的"1.0 基准"可供相乘，故配件直接给出绝对方向，
        // 不配置就完全不干预。这里只取原始值，与枪械 display 的取舍留到 resolveShot 里做。
        SmokeDirectionData.Config smokeDirectionConfig = data != null ? data.getSmokeDirectionConfig() : null;
        return new MuzzleAttachmentEffect(offsetBb, flashScale, smokeScale,
                smokeSpeedScale, smokeCountScale, smokeDirectionConfig,
                isSilenced(data), hasFlashHider);
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
     * id 为 null 表示 display 显式配置为不生成对应效果（闪光/烟雾 × 1p/3p）。
     */
    public record MuzzleShot(@Nullable ResourceLocation flashId, float flashScale, int flashLightLevel,
                             @Nullable ResourceLocation smokeId, float smokeScale, float offsetBb,
                             @Nullable ResourceLocation thirdPersonFlashId,
                             @Nullable ResourceLocation thirdPersonSmokeId,
                             float speedScale, float smokeParticleCount, float smokeSize,
                             @Nullable SmokeDirection smokeDirection) {
    }

    private record PendingShoot(@Nullable ResourceLocation flashId, float flashScale,
                                @Nullable ResourceLocation smokeId, float smokeScale, float offsetBb,
                                float speedScale, float smokeParticleCount, float smokeSize,
                                @Nullable SmokeDirection smokeDirection) {
    }

    private record BoundEmitter(ParticleEmitterInstance emitter, String locator, float offsetBb) {
    }

    private record MuzzleAttachmentEffect(float offsetBb, float flashScale, float smokeScale,
                                          float smokeSpeedScale, float smokeCountScale,
                                          @Nullable SmokeDirectionData.Config smokeDirectionConfig,
                                          boolean silenced, boolean hasFlashHider) {
        private static final MuzzleAttachmentEffect NONE =
                new MuzzleAttachmentEffect(0.0f, 1.0f, 1.0f, 1.0f, 1.0f, null, false, false);
    }

    /**
     * 枪烟发射方向：{@code dirs} 是一组归一化单位向量（每个粒子随机挑其中一个），
     * {@code spread} 为随机散布量。
     * 坐标系是配置用的直觉空间（+X 右、+Y 上、-Z 枪口前方），构造时已抵消渲染空间的翻转。
     * 为 {@code null} 表示配件不干预方向，粒子定义回退到默认的均匀四散。
     */
    record SmokeDirection(float[][] dirs, float spread, boolean angleMode) {
    }
}
