package com.tacz.guns.client.particle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import com.tacz.guns.config.client.RenderConfig;
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

/**
 * 第三人称枪口效果管理器：
 * <ul>
 *   <li>火光：世界粒子 + 锚点跟随（SBM 2.5.3 followAnchor）。开火时记录参数，首次第三人称
 *       持枪渲染创建发射器，窗口期内每帧把「当前枪口世界矩阵」喂给发射器
 *       （{@code updateEmitterAnchor}），粒子按矩阵每帧重定位——位置与朝向都跟随枪口，
 *       任意实体可见；</li>
 *   <li>烟雾：从枪口散开滞留的世界粒子（一次性，正确语义）。</li>
 * </ul>
 * 触发链路：客户端收到任意实体的开火（{@code GunFireEvent}，含服务器广播的他人开火）→
 * {@link #onShoot} 记录；第三人称持枪渲染（{@code ItemInHandLayer} → BEWLR
 * {@code renderByItem}）每帧计算该实体枪口的世界矩阵后 {@link #onMuzzleRendered} 更新。
 * <p>
 * 火光与烟雾的生命周期相互独立：烟雾在首次渲染时一次性消费；火光在窗口期内每帧更新锚点，
 * 窗口结束清理——不依赖烟雾的一次性消费，避免清理路径被提前短路。
 * <p>
 * 本机玩家自己的效果只在「自己的第三人称渲染」时被消费（第一人称视角下不渲染自己的
 * 持枪实体层，因此自己的效果仅第三人称可见）；其他玩家的效果在他们的持枪渲染时被消费，
 * 任何玩家视角都始终可见。
 */
public final class ThirdPersonMuzzleParticleManager {
    /** 火光跟随窗口（毫秒），与火光定义 max_lifetime 对应；窗口内每帧更新锚点，之后停止维护。 */
    private static final long FLASH_WINDOW_MS = 150;

    /** 待发烟雾请求：实体 → 该次开火的粒子参数（首次渲染时一次性消费）。 */
    private static final Map<LivingEntity, MuzzleParticleManager.MuzzleShot> PENDING = new WeakHashMap<>();

    /** 火光参数：实体 → 该次开火的火光参数（发射器创建后不再需要）。 */
    private static final Map<LivingEntity, MuzzleParticleManager.MuzzleShot> FLASH_PARAMS = new WeakHashMap<>();

    /** 火光发射器（锚点跟随）：实体 → 发射器，窗口期内每帧更新锚点矩阵。 */
    private static final Map<LivingEntity, ParticleEmitterInstance> FLASH_EMITTERS = new WeakHashMap<>();

    /** 火光窗口起始时间：实体 → 开火时刻（毫秒）。 */
    private static final Map<LivingEntity, Long> FLASH_START = new WeakHashMap<>();

    /**
     * 当前正在渲染第三人称持枪层的实体。由 {@code ItemInHandLayerMixin} 维护，
     * 供 BEWLR 渲染时关联到对应实体。
     */
    @Nullable
    public static LivingEntity currentEntity = null;

    private ThirdPersonMuzzleParticleManager() {
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
        FLASH_START.put(shooter, System.currentTimeMillis());
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
        long now = System.currentTimeMillis();

        // 火光：窗口内每帧更新锚点矩阵；窗口结束停止维护（粒子自然消亡）
        Long flashStart = FLASH_START.get(entity);
        if (flashStart != null) {
            if (now - flashStart >= FLASH_WINDOW_MS) {
                FLASH_START.remove(entity);
                FLASH_PARAMS.remove(entity);
                FLASH_EMITTERS.remove(entity);
            } else {
                ParticleEmitterInstance flashEmitter = FLASH_EMITTERS.get(entity);
                if (flashEmitter == null) {
                    // 首次渲染帧：用开火时记录的参数创建发射器，并把当前枪口矩阵设为初始锚点
                    MuzzleParticleManager.MuzzleShot shot = FLASH_PARAMS.get(entity);
                    if (shot != null && shot.flashScale() > 0) {
                        ParticleEffectDefinition def = ParticleDefinitionLoader.getInstance().getDefinition(shot.flashId());
                        if (def != null) {
                            Vec3 spawnPos = new Vec3(muzzleWorld.m30(), muzzleWorld.m31(), muzzleWorld.m32());
                            flashEmitter = WorldEmitterManager.getInstance().addEmitter(level, spawnPos, Vec3.ZERO, def);
                            if (flashEmitter != null) {
                                flashEmitter.setVariable("muzzle_scale", shot.flashScale());
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
        if (shot != null && RenderConfig.RENDER_MUZZLE_SMOKE.get() && shot.smokeScale() > 0 && shot.smokeId() != null) {
            spawnEffect(level, new Vec3(muzzleWorld.m30(), muzzleWorld.m31(), muzzleWorld.m32()),
                    shot.smokeId(), shot.smokeScale());
        }
    }

    private static void spawnEffect(ClientLevel level, Vec3 muzzleWorld, ResourceLocation definitionId, float scale) {
        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(definitionId);
        if (definition == null) {
            return;
        }
        // 交给 SBM 的世界发射器管理器：生命周期/每 tick 驱动/引擎投递全部由其接管
        ParticleEmitterInstance emitter = WorldEmitterManager.getInstance()
                .addEmitter(level, muzzleWorld, Vec3.ZERO, definition);
        if (emitter == null) {
            return;
        }
        emitter.setVariable("muzzle_scale", scale);
    }
}