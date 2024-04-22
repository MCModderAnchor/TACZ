package com.tac.guns.client.renderer.other;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.model.SlotModel;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.tac.guns.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MuzzleFlashRender {
    private static final SlotModel MUZZLE_FLASH_MODEL = new SlotModel(true);
    /**
     * 50ms 显示时间
     */
    private static final long TIME_RANGE = 50;

    private static long shootTimeStamp = -1;
    private static boolean muzzleFlashStartMark = false;
    private static float muzzleFlashRandomRotate = 0;
    private static Matrix3f muzzleFlashNormal = new Matrix3f();
    private static Matrix4f muzzleFlashPose = new Matrix4f();

    public static void render(ItemStack currentGunItem, PoseStack poseStack, BedrockModel bedrockModel) {
        if (OculusCompat.isRenderShadow()) {
            return;
        }
        long time = System.currentTimeMillis() - shootTimeStamp;
        if (time > TIME_RANGE) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(currentGunItem);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            MuzzleFlash muzzleFlash = gunIndex.getMuzzleFlash();
            if (muzzleFlash == null) {
                return;
            }
            if (muzzleFlashStartMark) {
                muzzleFlashNormal = poseStack.last().normal().copy();
                muzzleFlashPose = poseStack.last().pose().copy();
            }
            bedrockModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) -> {
                if (muzzleFlashNormal != null && muzzleFlashPose != null) {
                    float scale = 0.5f * muzzleFlash.getScale();
                    float scaleTime = TIME_RANGE / 2.0f;
                    scale = time < scaleTime ? (scale * (time / scaleTime)) : scale;
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(muzzleFlashNormal);
                    poseStack2.last().pose().multiply(muzzleFlashPose);
                    poseStack2.scale(scale, scale, scale);
                    poseStack2.mulPose(Vector3f.ZP.rotationDegrees(muzzleFlashRandomRotate));
                    poseStack2.translate(0, -1, 0);
                    muzzleFlashStartMark = false;
                    RenderType renderType = RenderType.itemEntityTranslucentCull(muzzleFlash.getTexture());
                    MultiBufferSource multiBufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                    MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderType), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            });
        });
    }

    public static void onShoot() {
        // 记录开火时间戳
        shootTimeStamp = System.currentTimeMillis();
        // 记录枪口火焰启动标记
        muzzleFlashStartMark = true;
        // 随机给予枪口火焰的旋转
        muzzleFlashRandomRotate = (float) (Math.random() * 360);
    }
}
