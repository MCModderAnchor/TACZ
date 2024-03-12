package com.tac.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.entity.EntityBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EntityBulletRenderer extends EntityRenderer<EntityBullet> {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation(GunMod.MOD_ID,"textures/entity/beam.png");
    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBullet pEntity) {
        return BEAM_LOCATION;
    }

    @Override
    public void render(EntityBullet bullet, float pEntityYaw, float pPartialTicks, PoseStack stack, MultiBufferSource buffer, int pPackedLight) {
        if(bullet.tickCount<1)return;

        stack.pushPose();
        {
            if(bullet.getOwner().equals(Minecraft.getInstance().player) && Minecraft.getInstance().options.getCameraType().isFirstPerson()){
                stack.translate(0, 0.08, 0);
            }

            Vec3 pos = bullet.getPosition(pPartialTicks);

            stack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(pPartialTicks,bullet.yRotO,bullet.getYRot())));
            stack.mulPose(Vector3f.XP.rotationDegrees(Mth.lerp(pPartialTicks,-bullet.xRotO,-bullet.getXRot())));

            float len = 4.0f;

            len = (float) Math.min(len+0.6f, bullet.getOwner().getEyePosition(pPartialTicks).distanceTo(pos)/1.1f);

            float outline = 0.004f;

            stack.translate(0, 0, 4.0f);

            VertexConsumer builder = buffer.getBuffer(RenderType.entitySolid(BEAM_LOCATION));

            renderPart(stack, builder, 1.0F, 1.0F, 1.0F, 1.0F, 0.01f, len, true);
            renderPart(stack, builder, 1.0F, 1.0F, 1.0F, 1.0F, 0.01f-outline,len-outline, false);

        }
        stack.popPose();

    }

    @Override
    public boolean shouldRender(EntityBullet pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }
    // 定义正方体的6个面，每个面由4个顶点组成
    private static final int[][] out = {{3, 2, 1, 0}, {2, 6, 5, 1}, {6, 7, 4, 5}, {7, 3, 0, 4}, {4, 0, 1, 5}, {3, 7, 6, 2}};
    private static final int[][] in = {{2, 3, 0, 1}, {6, 2, 1, 5}, {7, 6, 5, 4}, {3, 7, 4, 0}, {0, 4, 5, 1}, {7, 3, 2, 6}};
    // 定义正方体的8个顶点
    private static final float[][] vertices = {
            {-1.0f, -1.0f, -1.0f}, {1.0f, -1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}, {-1.0f, 1.0f, -1.0f},
            {-1.0f, -1.0f, 1.0f}, {1.0f, -1.0f, 1.0f}, {1.0f, 1.0f, 1.0f}, {-1.0f, 1.0f, 1.0f}
    };
    private static final float [][] uvs = {{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};
    private static void renderPart(PoseStack pPoseStack, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, float width, float len, boolean reverse) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        Matrix4f pPose = posestack$pose.pose();
        Matrix3f pNormal = posestack$pose.normal();

        int[][] faces = reverse ? in : out;
        // 对于每个面，添加4个顶点
        for (int[] face : faces) {
            for (int i=0;i<4;i++) {
                float[] vertex = vertices[face[i]];
                float[] uv = uvs[i];
                addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, vertex[0]*width, vertex[1]*width, vertex[2]*len,
                        (reverse ? 0 : 0.0625f)+uv[0]*0.0625f, uv[1]*0.0625f);
            }
        }
    }


    private static void addVertex(Matrix4f pPose, Matrix3f pNormal, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha,
                                  float x,float y, float z, float u, float v) {
        pConsumer.vertex(pPose, x,y,z)
                .color(pRed, pGreen, pBlue, pAlpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(pNormal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
