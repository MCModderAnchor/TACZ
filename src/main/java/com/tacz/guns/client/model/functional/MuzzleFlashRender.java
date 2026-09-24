package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

public class MuzzleFlashRender implements IFunctionalRenderer {
    private static final SlotModel MUZZLE_FLASH_MODEL = new SlotModel(true);
    /**
     * 50ms 显示时间
     */
    private static final long TIME_RANGE = 50;
    public static boolean isSelf = false;
    private static long shootTimeStamp = -1;
    private static float muzzleFlashRandomRotate = 0;
    private static Matrix3f muzzleFlashNormal = new Matrix3f();
    private static Matrix4f muzzleFlashPose = new Matrix4f();

    /**
     * 第三人称开火时间戳（按实体）：让附着在枪口骨骼上的四边形火光对任意射手生效，
     * 随枪械渲染逐帧附着（跟随枪口），其他玩家视角也始终可见。WeakHashMap 避免实体泄漏。
     */
    private static final Map<LivingEntity, Long> TP_SHOOT_TIMESTAMPS = new WeakHashMap<>();

    private final BedrockGunModel bedrockGunModel;

    public MuzzleFlashRender(BedrockGunModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    public static void onShoot() {
        // 记录开火时间戳
        shootTimeStamp = System.currentTimeMillis();
        // 随机给予枪口火焰的旋转
        muzzleFlashRandomRotate = (float) (Math.random() * 360);
    }

    /**
     * 记录某实体（任意射手）的第三人称开火时间戳，用于附着在枪口骨骼上的火光。
     */
    public static void onEntityShoot(LivingEntity entity) {
        TP_SHOOT_TIMESTAMPS.put(entity, System.currentTimeMillis());
    }

    private static void renderMuzzleFlash(GunDisplayInstance display, PoseStack poseStack, BedrockModel bedrockModel, long time, MultiBufferSource bufferSource) {
        MuzzleFlash muzzleFlash = display.getMuzzleFlash();
        if (muzzleFlash == null) {
            return;
        }
        // 每帧捕获当前枪口变换，使火光逐帧跟随枪口（后坐/摆动/瞄准均跟随）
        muzzleFlashNormal = new Matrix3f(poseStack.last().normal());
        muzzleFlashPose = new Matrix4f(poseStack.last().pose());
        bedrockModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) -> doRender(light, overlay, muzzleFlash, time, bufferSource));
    }

    private static void doRender(int light, int overlay, MuzzleFlash muzzleFlash, long time, MultiBufferSource bufferSource) {
        if (muzzleFlashNormal != null && muzzleFlashPose != null) {
            float scale = 0.5f * muzzleFlash.getScale();
            float scaleTime = TIME_RANGE / 2.0f;
            scale = time < scaleTime ? (scale * (time / scaleTime)) : scale;
            MultiBufferSource multiBufferSource = bufferSource;

            // 推送到指定位置
            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(muzzleFlashNormal);
            poseStack2.last().pose().mul(muzzleFlashPose);

            // 先渲染一遍半透明背景
            poseStack2.pushPose();
            {
                poseStack2.scale(scale, scale, scale);
                poseStack2.mulPose(Axis.ZP.rotationDegrees(muzzleFlashRandomRotate));
                poseStack2.translate(0, -1, 0);
                RenderType renderTypeBg = RenderType.entityTranslucent(muzzleFlash.getTexture());
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeBg), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();

            // 然后渲染发光效果
            poseStack2.pushPose();
            {
                poseStack2.scale(scale / 2, scale / 2, scale / 2);
                poseStack2.mulPose(Axis.ZP.rotationDegrees(muzzleFlashRandomRotate));
                poseStack2.translate(0, -0.9, 0);
                RenderType renderTypeLight = RenderType.energySwirl(muzzleFlash.getTexture(), 1, 1);
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeLight), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
//        render(poseStack, vertexBuffer, transformType, light, overlay, Minecraft.getInstance().renderBuffers().bufferSource());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        if (OculusCompat.isRenderShadow()) {
            return;
        }
        long time;
        if (transformType.firstPerson()) {
            // 第一人称：仅本机玩家开火；renew 枪械由 FP 粒子系统表现
            if (!isSelf) {
                return;
            }
            if (shootTimeStamp < 0) {
                return;
            }
            time = System.currentTimeMillis() - shootTimeStamp;
            if (time > TIME_RANGE) {
                return;
            }
        } else {
            // 第三人称：任意实体（本机 F5 + 其他玩家），按实体计时，附着枪口渲染（跟随枪口）
            LivingEntity entity = ThirdPersonMuzzleParticleManager.currentEntity;
            if (entity == null) {
                return;
            }
            Long ts = TP_SHOOT_TIMESTAMPS.get(entity);
            if (ts == null) {
                return;
            }
            time = System.currentTimeMillis() - ts;
            if (time > TIME_RANGE) {
                TP_SHOOT_TIMESTAMPS.remove(entity);
                return;
            }
        }
        ItemStack currentGunItem = bedrockGunModel.getCurrentGunItem();

        TimelessAPI.getGunDisplay(currentGunItem).ifPresent(display -> {
            MuzzleFlash muzzleFlash = display.getMuzzleFlash();
            if (muzzleFlash == null) {
                return;
            }
            // renew 枪械的枪口火光由 snowstorm 粒子表现（第一人称 FP 粒子 / 第三人称锚点跟随世界粒子），
            // 不再渲染旧四边形火光；default 枪械保留四边形火光（第一人称本机 + 第三人称按实体）
            if (muzzleFlash.isRenew()) {
                return;
            }
            ItemStack muzzleAttachment = bedrockGunModel.getCurrentAttachmentItem().get(AttachmentType.MUZZLE);
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(muzzleAttachment);
            if (iAttachment != null) {
                ResourceLocation attachmentId = iAttachment.getAttachmentId(muzzleAttachment);
                TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> {
                    var modifier = index.getData().getModifier();
                    if (modifier.containsKey(SilenceModifier.ID) && modifier.get(SilenceModifier.ID).getValue() instanceof Pair<?, ?> pair) {
                        // 如果安装了消音器，则不渲染枪口火光
                        if (((Pair<Integer, Boolean>) pair).right()) {
                            return;
                        }
                    }
                    renderMuzzleFlash(display, poseStack, bedrockGunModel, time, bufferSource);
                });
            } else {
                renderMuzzleFlash(display, poseStack, bedrockGunModel, time, bufferSource);
            }
        });
    }
}
