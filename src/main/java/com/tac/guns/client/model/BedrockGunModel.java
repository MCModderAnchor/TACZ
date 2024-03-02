package com.tac.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.tac.guns.client.model.CommonComponents.*;

public class BedrockGunModel extends BedrockAnimatedModel {
    public static final String IRON_VIEW_NODE = "iron_view";
    public static final String IDLE_VIEW_NODE = "idle_view";
    public static final String THIRD_PERSON_HAND_ORIGIN_NODE = "thirdperson_hand";
    public static final String FIXED_ORIGIN_NODE = "fixed";
    public static final String GROUND_ORIGIN_NODE = "ground";
    public static final String SCOPE_POS_NODE = "scope_pos";
    public static final String EJECTION_NODE = "ejection";
    private final SecondOrderDynamics aimingDynamics = new SecondOrderDynamics(0.45f, 0.8f, 1.2f, 0);
    // 第一人称机瞄摄像机定位组的路径
    protected @Nullable List<BedrockPart> ironSightPath;
    // 第一人称idle状态摄像机定位组的路径
    protected @Nullable List<BedrockPart> idleSightPath;
    // 第三人称手部物品渲染原点定位组的路径
    protected @Nullable List<BedrockPart> thirdPersonHandOriginPath;
    // 展示框渲染原点定位组的路径
    protected @Nullable List<BedrockPart> fixedOriginPath;
    // 地面实体渲染原点定位组的路径
    protected @Nullable List<BedrockPart> groundOriginPath;
    // 瞄具配件定位组的路径。其他配件不需要存路径，只需要替换渲染。但是瞄具定位组需要用来辅助第一人称瞄准的摄像机定位。
    protected @Nullable List<BedrockPart> scopePosPath;
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
            // todo 枪内有弹则渲染
            return null;
        });
        this.setFunctionalRenderer(BULLET_IN_MAG, bedrockPart -> {
            // todo 枪内子弹数大于 1 则渲染
            return null;
        });
        this.setFunctionalRenderer(BULLET_CHAIN, bedrockPart -> {
            // todo 枪内有弹则渲染
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

        ironSightPath = getPath(modelMap.get(IRON_VIEW_NODE));
        idleSightPath = getPath(modelMap.get(IDLE_VIEW_NODE));
        thirdPersonHandOriginPath = getPath(modelMap.get(THIRD_PERSON_HAND_ORIGIN_NODE));
        fixedOriginPath = getPath(modelMap.get(FIXED_ORIGIN_NODE));
        groundOriginPath = getPath(modelMap.get(GROUND_ORIGIN_NODE));
        scopePosPath = getPath(modelMap.get(SCOPE_POS_NODE));

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

    @Nullable
    public List<BedrockPart> getIronSightPath() {
        return ironSightPath;
    }

    @Nullable
    public List<BedrockPart> getIdleSightPath() {
        return idleSightPath;
    }

    @Nullable
    public List<BedrockPart> getThirdPersonHandOriginPath() {
        return thirdPersonHandOriginPath;
    }

    @Nullable
    public List<BedrockPart> getFixedOriginPath() {
        return fixedOriginPath;
    }

    @Nullable
    public List<BedrockPart> getGroundOriginPath() {
        return groundOriginPath;
    }

    @Nullable
    public List<BedrockPart> getScopePosPath() {
        return scopePosPath;
    }

    public void render(float partialTicks, ItemTransforms.TransformType transformType, ItemStack stack, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        currentItem = stack;
        currentEntity = entity;
        //调用上层渲染方法
        render(transformType, matrixStack, buffer, light, overlay);
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

    private List<BedrockPart> getPath(ModelRendererWrapper rendererWrapper) {
        if (rendererWrapper == null) {
            return null;
        }
        BedrockPart part = rendererWrapper.getModelRenderer();
        List<BedrockPart> path = new ArrayList<>();
        Stack<BedrockPart> stack = new Stack<>();
        do {
            stack.push(part);
            part = part.getParent();
        } while (part != null);
        while (!stack.isEmpty()) {
            part = stack.pop();
            path.add(part);
        }
        return path;
    }
}
