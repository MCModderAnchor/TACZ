package com.tacz.guns.client.particle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;


public final class ThirdPersonMuzzleParticleManager {
    private static final long FLASH_WINDOW_MS = 150;

    private static final Map<LivingEntity, MuzzleParticleManager.MuzzleShot> PENDING = new WeakHashMap<>();

    private static final Map<LivingEntity, MuzzleParticleManager.MuzzleShot> FLASH_PARAMS = new WeakHashMap<>();

    private static final Map<LivingEntity, ParticleEmitterInstance> FLASH_EMITTERS = new WeakHashMap<>();

    private static final Map<LivingEntity, Long> FLASH_START = new WeakHashMap<>();

    /**
     * 当前正在渲染第三人称持枪层的实体。由 {@code ItemInHandLayerMixin} 维护，
     * 供 BEWLR 渲染时关联到对应实体。
     */
    @Nullable
    public static LivingEntity currentEntity = null;

    private ThirdPersonMuzzleParticleManager() {
    }

    public static void resetTimestamps() {
        PENDING.clear();
        FLASH_PARAMS.clear();
        FLASH_EMITTERS.clear();
        FLASH_START.clear();
        currentEntity = null;
    }

    /**
     * 客户端开火事件调用：任何射手（本机玩家 + 服务器广播的其他玩家）。
     * <p>
     * 视角门控：本机玩家在第一人称视角下不渲染其第三人称持枪层，若此时入队会在
     * 切回第三人称时一次性堆积全发；因此本机玩家的请求仅在非第一人称视角时入队。
     * 其他玩家（服务器广播）始终通过其第三人称持枪渲染消费，与本机视角无关。
     */
    public static void onShoot(LivingEntity shooter, ItemStack gunItem) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (shooter.equals(player) && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        MuzzleParticleManager.MuzzleShot shot = MuzzleParticleManager.resolveShot(gunItem);
        if (shot == null) {
            return;
        }
        PENDING.put(shooter, shot);
        FLASH_PARAMS.put(shooter, shot);
        FLASH_START.put(shooter, GunTime.nowMillis());
    }

    /**
     * 第三人称持枪渲染时调用：更新该实体枪口效果。
     * <p>
     * 火光：窗口期内每帧创建/更新锚点跟随发射器（位置+朝向跟随枪口），
     * 窗口结束清理发射器维护（粒子自然消亡）；不依赖烟雾的一次性消费。
     * 烟雾：一次性，首次渲染时在枪口世界坐标发射。
     *
     * @param muzzleWorld 当前枪口世界矩阵（旋转+平移）
     */
    public static void onMuzzleRendered(LivingEntity entity, Matrix4f muzzleWorld) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = GunTime.nowMillis();

        // 火光：窗口内每帧更新锚点矩阵；窗口结束停止维护（粒子自然消亡）
        Long flashStart = FLASH_START.get(entity);
        if (flashStart != null) {
            if (now < flashStart || now - flashStart >= FLASH_WINDOW_MS) {
                FLASH_START.remove(entity);
                FLASH_PARAMS.remove(entity);
                FLASH_EMITTERS.remove(entity);
            } else {
                ParticleEmitterInstance flashEmitter = FLASH_EMITTERS.get(entity);
                if (flashEmitter == null) {
                    // 首次渲染帧：用开火时记录的参数创建发射器，并把当前枪口矩阵设为初始锚点
                    MuzzleParticleManager.MuzzleShot shot = FLASH_PARAMS.get(entity);
                    // id 为 null 表示 display 显式配置为不生成 3p 火光
                    if (shot != null && shot.thirdPersonFlashId() != null && shot.flashScale() > 0) {
                        ParticleEffectDefinition def = ParticleDefinitionLoader.getInstance().getDefinition(shot.thirdPersonFlashId());
                        if (def != null) {
                            Vec3 spawnPos = new Vec3(muzzleWorld.m30(), muzzleWorld.m31(), muzzleWorld.m32());
                            flashEmitter = WorldEmitterManager.getInstance().addEmitter(level, spawnPos, Vec3.ZERO, def);
                            if (flashEmitter != null) {
                                flashEmitter.setVariable("muzzle_scale", shot.flashScale());
                                flashEmitter.setVariable("muzzle_speed", shot.speedScale());
                                flashEmitter.setFollowAnchor(true);
                                FLASH_EMITTERS.put(entity, flashEmitter);
                                // 立即写入初始锚点（含旋转），保证首帧粒子就锚定在枪口
                                WorldEmitterManager.getInstance().updateEmitterAnchor(flashEmitter, muzzleWorld);
                            }
                        }
                    }
                } else {
                    // 发射器可能已自然结束并从管理器移除：更新失败则丢弃引用，等待下次开火重建
                    if (!WorldEmitterManager.getInstance().updateEmitterAnchor(flashEmitter, muzzleWorld)) {
                        FLASH_EMITTERS.remove(entity);
                    }
                }
            }
        }

        // 烟雾：一次性消费（与火光生命周期无关）
        MuzzleParticleManager.MuzzleShot shot = PENDING.remove(entity);
        // id 为 null 表示 display 显式配置为不生成 3p 烟雾
        if (shot != null && RenderConfig.RENDER_MUZZLE_SMOKE.get() && shot.thirdPersonSmokeId() != null && shot.smokeScale() > 0) {
            spawnEffect(level, muzzleWorld,
                    shot.thirdPersonSmokeId(), shot.smokeScale(), shot.speedScale(),
                    shot.smokeParticleCount(), shot.smokeSize(), shot.smokeDirection());
        }
    }

    /**
     * @param muzzleWorld 枪口世界矩阵（含旋转），不能只取平移分量——粒子的发射方向要经它变换到世界空间，
     *                    丢掉旋转后方向就成了世界坐标里定死的一条，枪一转就跟不上。
     */
    private static void spawnEffect(ClientLevel level, Matrix4f muzzleWorld, ResourceLocation definitionId,
                                    float scale, float speedScale, float smokeParticleCount, float smokeSize,
                                    @Nullable MuzzleParticleManager.SmokeDirection smokeDirection) {
        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(definitionId);
        if (definition == null) {
            return;
        }
        // 交给 SBM 的世界发射器管理器：生命周期/每 tick 驱动/引擎投递全部由其接管
        ParticleEmitterInstance emitter = WorldEmitterManager.getInstance()
                .addEmitter(level, new Vec3(muzzleWorld.m30(), muzzleWorld.m31(), muzzleWorld.m32()), Vec3.ZERO, definition);
        if (emitter == null) {
            return;
        }
        // addEmitter 只给了位置，旋转要在这里补上：不补的话 emitter 的 worldTransform 停在单位阵，
        // 粒子方向就落在世界坐标里，不跟随枪口朝向。
        // 轴系与第一人称一致：第三人称的枪体 poseStack 经过 GunItemTransforms 的 scale(-1, -1, 1)，
        // 与第一人称那句 rotateZ(180°) 是同一个变换（(x,y,z) → (-x,-y,z)），
        // 所以 MuzzleShot 里已经抵消过翻转的方向可以直接用，不必再翻一次。
        WorldEmitterManager.getInstance().updateEmitterAnchor(emitter, muzzleWorld);
        // 与枪焰使用互不重叠的 smoke_* 命名空间（见 MuzzleParticleManager#createSmokeEmitter）
        emitter.setVariable("smoke_size", smokeSize);
        emitter.setVariable("smoke_vel", speedScale);
        emitter.setVariable("smoke_count", smokeParticleCount);
        MuzzleParticleManager.writeSmokeDirectionVariables(emitter, smokeDirection);
    }
}
