package com.tac.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.model.bedrock.ModelRendererWrapper;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.tac.guns.client.model.CommonComponents.*;

public class BedrockGunModel extends BedrockAnimatedModel {
    public static final String IRON_VIEW_NODE = "iron_view";
    public static final String SCOPE_POS_NODE = "scope_pos";
    public static final String EJECTION_NODE = "ejection";
    protected final List<BedrockPart> ironSightPath = new ArrayList<>();
    protected final List<BedrockPart> scopePosPath = new ArrayList<>();
    private final SecondOrderDynamics aimingDynamics = new SecondOrderDynamics(0.45f, 0.8f, 1.2f, 0);
    protected ItemStack currentItem;
    protected LivingEntity currentEntity;
    protected Matrix4f ejectionPose = null;
    protected Matrix3f ejectionNormal = null;
    protected Vector3f ejectionVelocity = null;
    protected Vector3f ejectionRandomVelocity = null;
    protected Vector3f ejectionAngularVelocity = null;
    protected float ejectionLivingTimeS = 0;

    public BedrockGunModel(BedrockModelPOJO pojo, BedrockVersion version, RenderType renderType) {
        super(pojo, version, renderType);
        this.setFunctionalRenderer("LeftHand", bedrockPart -> (poseStack, transformType, consumer, light, overlay) -> {
            if (transformType.firstPerson()) {
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack2, Minecraft.getInstance().renderBuffers().bufferSource(), light1);
                });
            }
        });
        this.setFunctionalRenderer("RightHand", bedrockPart -> (poseStack, transformType, consumer, light, overlay) -> {
            if (transformType.firstPerson()) {
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.RIGHT, poseStack2, Minecraft.getInstance().renderBuffers().bufferSource(), light1);
                });
            }
        });
        this.setFunctionalRenderer(BULLET_IN_BARREL, bedrockPart -> {
            CompoundTag tag = currentItem.getOrCreateTag();
            int ammoCount = tag.getInt("AmmoCount");
            bedrockPart.visible = ammoCount != 0;
            return null;
        });
        this.setFunctionalRenderer(BULLET_IN_MAG, bedrockPart -> {
            CompoundTag tag = currentItem.getOrCreateTag();
            int ammoCount = tag.getInt("AmmoCount");
            bedrockPart.visible = ammoCount > 1;
            return null;
        });
        this.setFunctionalRenderer(BULLET_CHAIN, bedrockPart -> {
            CompoundTag tag = currentItem.getOrCreateTag();
            int ammoCount = tag.getInt("AmmoCount");
            bedrockPart.visible = ammoCount != 0;
            return null;
        });
        this.setFunctionalRenderer(MUZZLE_BRAKE, bedrockPart -> {
            //todo 判断枪械是否安装枪口制动器，改变bedrockPart.visible以隐藏或者显示对应模型，下同
            return null;
        });
        this.setFunctionalRenderer(MUZZLE_COMPENSATOR, bedrockPart -> {
            //todo 安装补偿器时可见
            return null;
        });
        this.setFunctionalRenderer(MUZZLE_SILENCER, bedrockPart -> {
            //todo 安装消音器时可见
            return null;
        });
        this.setFunctionalRenderer(MUZZLE_DEFAULT, bedrockPart -> {
            //todo 没有枪口配件时可见
            return null;
        });
        this.setFunctionalRenderer(MOUNT, bedrockPart -> {
            //todo 安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(CARRY, bedrockPart -> {
            //todo 未安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(SIGHT_FOLDED, bedrockPart -> {
            //todo 安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(SIGHT, bedrockPart -> {
            //todo 未安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(STOCK_LIGHT, bedrockPart -> {
            //todo 安装轻型枪托时可见
            return null;
        });
        this.setFunctionalRenderer(STOCK_TACTICAL, bedrockPart -> {
            //todo 安装战术枪托时可见
            return null;
        });
        this.setFunctionalRenderer(STOCK_HEAVY, bedrockPart -> {
            //todo 安装重型枪托时可见
            return null;
        });
        this.setFunctionalRenderer(STOCK_DEFAULT, bedrockPart -> {
            //todo 未安装枪托时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_1, bedrockPart -> {
            //todo 安装一级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_2, bedrockPart -> {
            //todo 安装二级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_3, bedrockPart -> {
            //todo 安装三级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_STANDARD, bedrockPart -> {
            //todo 未安装扩容弹匣时可见
            return null;
        });
        {
            ModelRendererWrapper rendererWrapper = modelMap.get(IRON_VIEW_NODE);
            if (rendererWrapper != null) {
                BedrockPart it = rendererWrapper.getModelRenderer();
                Stack<BedrockPart> stack = new Stack<>();
                do {
                    stack.push(it);
                    it = it.getParent();
                } while (it != null);
                while (!stack.isEmpty()) {
                    it = stack.pop();
                    ironSightPath.add(it);
                }
            }
        }
        this.setFunctionalRenderer(IRON_VIEW_NODE, bedrockPart -> {
            bedrockPart.visible = false;
            return null;
        });
        {
            ModelRendererWrapper rendererWrapper = modelMap.get(SCOPE_POS_NODE);
            if (rendererWrapper != null) {
                BedrockPart it = rendererWrapper.getModelRenderer();
                Stack<BedrockPart> stack = new Stack<>();
                do {
                    stack.push(it);
                    it = it.getParent();
                } while (it != null);
                while (!stack.isEmpty()) {
                    it = stack.pop();
                    scopePosPath.add(it);
                }
            }
        }
        this.setFunctionalRenderer(SCOPE_POS_NODE, bedrockPart -> {
            bedrockPart.visible = false;
            return (poseStack, transformType, consumer, light, overlay) -> {
                //todo 获取当前枪械安装的瞄具，获取其模型并渲染。注释的代码先保留
                /*
                ItemStack scopeItemStack = Gun.getAttachment(IAttachment.Type.SCOPE, currentItem);
                if (!scopeItemStack.isEmpty()) {
                    IOverrideModel scopeModel = OverrideModelManager.getModel(scopeItemStack);
                    if (scopeModel instanceof BedrockAttachmentModel bedrockScopeModel) {
                        Matrix3f normal = poseStack.last().normal().copy();
                        Matrix4f pose = poseStack.last().pose().copy();
                        //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                        this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                            PoseStack poseStack2 = new PoseStack();
                            poseStack2.last().normal().mul(normal);
                            poseStack2.last().pose().multiply(pose);
                            //从bedrock model的渲染原点(0, 24, 0)移动到模型原点(0, 0, 0)
                            poseStack2.translate(0, -1.5f, 0);
                            bedrockScopeModel.render(transformType1, poseStack2, Minecraft.getInstance().renderBuffers().bufferSource(), light1, overlay1);
                        });
                    }
                }
                */
            };
        });
        this.setFunctionalRenderer(EJECTION_NODE, bedrockPart -> {
            bedrockPart.visible = false;
            return (poseStack, transformType, consumer, light, overlay) -> {
                ejectionPose = poseStack.last().pose();
                ejectionNormal = poseStack.last().normal();
            };
        });
        for (ModelRendererWrapper rendererWrapper : modelMap.values()) {
            if (rendererWrapper.getModelRenderer().name != null && rendererWrapper.getModelRenderer().name.endsWith("_illuminated")) {
                rendererWrapper.getModelRenderer().illuminated = true;
            }
        }
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

    public float getEjectionLivingTimeS() {
        return ejectionLivingTimeS;
    }

    public Vector3f getEjectionVelocity() {
        return ejectionVelocity;
    }

    public Vector3f getEjectionRandomVelocity() {
        return ejectionRandomVelocity;
    }

    public Vector3f getEjectionAngularVelocity() {
        return ejectionAngularVelocity;
    }

    public Matrix4f getEjectionPose() {
        return ejectionPose;
    }

    public Matrix3f getEjectionNormal() {
        return ejectionNormal;
    }

    public void render(float partialTicks, ItemTransforms.TransformType transformType, ItemStack stack, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        currentItem = stack;
        currentEntity = entity;

        matrixStack.pushPose();

        if (transformType.firstPerson()) {
            //todo v就是瞄准动作的进度
            float v = 0;
            //从渲染原点(0, 24, 0)移动到模型原点(0, 0, 0)
            matrixStack.translate(0, 1.5f, 0);
            //游戏中模型是上下颠倒的，需要翻转过来。
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            //todo 判断是否安装瞄具，上半部分是未安装瞄具时，根据机瞄定位组"iron_sight"应用位移。下半部分是安装瞄具时，应用瞄具定位组和瞄具模型内定位组的位移。不要删掉注释的代码.
            if (true) {
                //应用定位组的反向位移、旋转，使定位组的位置就是屏幕中心
                matrixStack.translate(0, 1.5f, 0);
                for (int f = ironSightPath.size() - 1; f >= 0; f--) {
                    BedrockPart t = ironSightPath.get(f);
                    float[] q = toQuaternion(-t.xRot * v, -t.yRot * v, -t.zRot * v);
                    matrixStack.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
                    if (t.getParent() != null)
                        matrixStack.translate(-t.x / 16.0F * v, -t.y / 16.0F * v, -t.z / 16.0F * v);
                    else {
                        matrixStack.translate(-t.x / 16.0F * v, (1.5F - t.y / 16.0F) * v, -t.z / 16.0F * v);
                    }
                }
                matrixStack.translate(0, -1.5f, 0);
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
            matrixStack.translate(0, 0.5 * (1 - v), -0.75 * (1 - v));
        }
        //调用上层渲染方法
        render(transformType, matrixStack, buffer, light, overlay);
        matrixStack.popPose();
    }

    private void renderFirstPersonArm(LocalPlayer player, HumanoidArm hand, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        PlayerRenderer renderer = (PlayerRenderer) renderManager.getRenderer(player);
        int oldId = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, player.getSkinTextureLocation());

        if (hand == HumanoidArm.RIGHT) {
            renderer.renderRightHand(matrixStack, buffer, combinedLight, player);
        } else {
            renderer.renderLeftHand(matrixStack, buffer, combinedLight, player);
        }
        RenderSystem.setShaderTexture(0, oldId);
    }
}
