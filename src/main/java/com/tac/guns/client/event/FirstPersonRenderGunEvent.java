package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        LocalPlayer player = mc.player;
        InteractionHand hand = event.getHand();
        ItemStack stack = event.getItemStack();
        ItemTransforms.TransformType transformType = hand == InteractionHand.MAIN_HAND ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
        if (stack.is(ModItems.GUN.get())) {
            ClientGunIndex gunIndex = ClientGunLoader.getGunIndex(GunItem.DEFAULT);
            BedrockGunModel gunModel = gunIndex.getGunModel();
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (gunModel != null) {
                // 在渲染之前，先更新动画，让动画数据写入模型
                if (animationStateMachine != null) {
                    animationStateMachine.update();
                }
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                // 从渲染原点(0, 24, 0)移动到模型原点(0, 0, 0)
                poseStack.translate(0, 1.5f, 0);
                // 基岩版模型是上下颠倒的，需要翻转过来。
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                // 应用枪械动态，如取消原版的bobbing、瞄准时的位移、后坐力的位移等
                applyFirstPersonGunMoving(player, stack, gunIndex, poseStack, gunModel);
                // 调用模型渲染
                gunModel.render(0, transformType, stack, player, poseStack, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
                poseStack.popPose();
                gunModel.cleanAnimationTransform();
                event.setCanceled(true);
            }
        }
    }

    /**
     * 当主手拿着枪械物品的时候，取消应用在它上面的 viewBobbing，以便应用自定义的跑步/走路动画。
     */
    @SubscribeEvent
    public static void cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event){
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.is(ModItems.GUN.get())) {
            event.setCanceled(true);
        }
    }

    private static void applyFirstPersonGunMoving(LocalPlayer player,
                                                  ItemStack gunItemStack,
                                                  ClientGunIndex gunIndex,
                                                  PoseStack poseStack,
                                                  BedrockGunModel model){
        applyAimingTransform(gunIndex, poseStack, model);
    }

    private static void applyAimingTransform(ClientGunIndex gunIndex, PoseStack poseStack, BedrockGunModel model){
        //todo v就是瞄准动作的进度
        float v = 0;
        //todo 判断是否安装瞄具，上半部分是未安装瞄具时，根据机瞄定位组"iron_sight"应用位移。下半部分是安装瞄具时，应用瞄具定位组和瞄具模型内定位组的位移。不要删掉注释的代码.
        if (true) {
            List<BedrockPart> ironSightPath = model.getIronSightPath();
            //应用定位组的反向位移、旋转，使定位组的位置就是屏幕中心
            poseStack.translate(0, 1.5f, 0);
            for (int f = ironSightPath.size() - 1; f >= 0; f--) {
                BedrockPart t = ironSightPath.get(f);
                float[] q = toQuaternion(-t.xRot * v, -t.yRot * v, -t.zRot * v);
                poseStack.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
                if (t.getParent() != null)
                    poseStack.translate(-t.x / 16.0F * v, -t.y / 16.0F * v, -t.z / 16.0F * v);
                else {
                    poseStack.translate(-t.x / 16.0F * v, (1.5F - t.y / 16.0F) * v, -t.z / 16.0F * v);
                }
            }
            poseStack.translate(0, -1.5f, 0);
        } else {
                /*
                IOverrideModel scopeModel = OverrideModelManager.getModel(scopeItemStack);
                if (scopeModel instanceof BedrockAttachmentModel bedrockScopeModel) {
                    //应用定位组的反向位移、旋转，使定位组的位置就是屏幕中心
                    matrixStack.translate(0, 1.5f, 0);
                    for (int f = bedrockScopeModel.scopeViewPath.size() - 1; f >= 0; f--) {
                        BedrockPart t = bedrockScopeModel.scopeViewPath.get(f);
                        float[] q = toQuaternion(-t.xRot * v, -t.yRot * v, -t.zRot * v);
                        matrixStack.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
                        if (t.getParent() != null)
                            matrixStack.translate(-t.x / 16.0F * v, -t.y / 16.0F * v, -t.z / 16.0F * v);
                        else {
                            matrixStack.translate(-t.x / 16.0F * v, (1.5F - t.y / 16.0F) * v, -t.z / 16.0F * v);
                        }
                    }
                    for (int f = scopePosPath.size() - 1; f >= 0; f--) {
                        BedrockPart t = scopePosPath.get(f);
                        float[] q = toQuaternion(-t.xRot * v, -t.yRot * v, -t.zRot * v);
                        matrixStack.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
                        if (t.getParent() != null)
                            matrixStack.translate(-t.x / 16.0F * v, -t.y / 16.0F * v, -t.z / 16.0F * v);
                        else {
                            matrixStack.translate(-t.x / 16.0F * v, (1.5F - t.y / 16.0F) * v, -t.z / 16.0F * v);
                        }
                    }
                    matrixStack.translate(0, -1.5f, 0);
                }
                */
        }
        //主摄像机的默认位置是(0, 8, 12)
        poseStack.translate(0, 0.5 * (1 - v), -0.75 * (1 - v));
    }


    private static float[] toQuaternion(float roll, float pitch, float yaw) {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        return new float[]{
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        };
    }
}
