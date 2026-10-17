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

        Long flashStart = FLASH_START.get(entity);
        if (flashStart != null) {
            if (now < flashStart || now - flashStart >= FLASH_WINDOW_MS) {
                FLASH_START.remove(entity);
                FLASH_PARAMS.remove(entity);
                FLASH_EMITTERS.remove(entity);
            } else {
                ParticleEmitterInstance flashEmitter = FLASH_EMITTERS.get(entity);
                if (flashEmitter == null) {
                    MuzzleParticleManager.MuzzleShot shot = FLASH_PARAMS.get(entity);
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
                                WorldEmitterManager.getInstance().updateEmitterAnchor(flashEmitter, muzzleWorld);
                            }
                        }
                    }
                } else {
                    if (!WorldEmitterManager.getInstance().updateEmitterAnchor(flashEmitter, muzzleWorld)) {
                        FLASH_EMITTERS.remove(entity);
                    }
                }
            }
        }

        // 烟雾：一次性消费（与火光生命周期无关）
        MuzzleParticleManager.MuzzleShot shot = PENDING.remove(entity);

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

        ParticleEmitterInstance emitter = WorldEmitterManager.getInstance()
                .addEmitter(level, new Vec3(muzzleWorld.m30(), muzzleWorld.m31(), muzzleWorld.m32()), Vec3.ZERO, definition);
        if (emitter == null) {
            return;
        }

        WorldEmitterManager.getInstance().updateEmitterAnchor(emitter, muzzleWorld);
        emitter.setVariable("smoke_size", smokeSize);
        emitter.setVariable("smoke_vel", speedScale);
        emitter.setVariable("smoke_count", smokeParticleCount);
        MuzzleParticleManager.writeSmokeDirectionVariables(emitter, smokeDirection);
    }
}
