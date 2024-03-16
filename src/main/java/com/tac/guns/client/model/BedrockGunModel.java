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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

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
    public static final String CONSTRAINT_NODE = "constraint";
    public static final String REFIT_VIEW_NODE = "refit_view";
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
    // 动画约束组的路径
    protected @Nullable List<BedrockPart> constraintPath;
    // 枪械改装 Overview 视角定位组路径
    protected @Nullable List<BedrockPart> refitViewPath;
    private ISimpleRenderer scopeRenderer;
    private boolean renderHand = true;

    public BedrockGunModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        this.setFunctionalRenderer("lefthand_pos", bedrockPart -> (poseStack, transformType, consumer, light, overlay) -> {
            if (transformType.firstPerson()) {
                if (!renderHand) {
                    return;
                }
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack2, light1);
                });
            }
        });
        this.setFunctionalRenderer("righthand_pos", bedrockPart -> (poseStack, transformType, consumer, light, overlay) -> {
            if (transformType.firstPerson()) {
                if (!renderHand) {
                    return;
                }
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.RIGHT, poseStack2, light1);
                });
            }
        });
        this.setFunctionalRenderer(BULLET_IN_BARREL, bedrockPart -> {
            //TODO 枪内有弹则渲染
            return null;
        });
        this.setFunctionalRenderer(BULLET_IN_MAG, bedrockPart -> {
            //TODO 枪内子弹数大于 1 则渲染
            return null;
        });
        this.setFunctionalRenderer(BULLET_CHAIN, bedrockPart -> {
            //TODO 枪内有弹则渲染
            return null;
        });
        this.setFunctionalRenderer(MOUNT, bedrockPart -> {
            bedrockPart.visible = (scopeRenderer != null);
            return null;
        });
        this.setFunctionalRenderer(CARRY, bedrockPart -> {
            //TODO 未安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(SIGHT_FOLDED, bedrockPart -> {
            //TODO 安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(SIGHT, bedrockPart -> {
            //TODO 未安装瞄具时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_1, bedrockPart -> {
            //TODO 安装一级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_2, bedrockPart -> {
            //TODO 安装二级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_3, bedrockPart -> {
            //TODO 安装三级扩容弹匣时可见
            return null;
        });
        this.setFunctionalRenderer(MAG_STANDARD, bedrockPart -> {
            //TODO 未安装扩容弹匣时可见
            return null;
        });

        ironSightPath = getPath(modelMap.get(IRON_VIEW_NODE));
        idleSightPath = getPath(modelMap.get(IDLE_VIEW_NODE));
        thirdPersonHandOriginPath = getPath(modelMap.get(THIRD_PERSON_HAND_ORIGIN_NODE));
        fixedOriginPath = getPath(modelMap.get(FIXED_ORIGIN_NODE));
        groundOriginPath = getPath(modelMap.get(GROUND_ORIGIN_NODE));
        scopePosPath = getPath(modelMap.get(SCOPE_POS_NODE));
        constraintPath = getPath(modelMap.get(CONSTRAINT_NODE));
        refitViewPath = getPath(modelMap.get(REFIT_VIEW_NODE));

        this.setFunctionalRenderer(SCOPE_POS_NODE, bedrockPart -> {
            bedrockPart.visible = false;
            return (poseStack, transformType, consumer, light, overlay) -> {
                if(scopeRenderer != null){
                    Matrix3f normal = poseStack.last().normal().copy();
                    Matrix4f pose = poseStack.last().pose().copy();
                    //和枪械模型共用缓冲区的都需要代理到渲染结束后渲染
                    this.delegateRender((poseStack1, transformType1, consumer1, light1, overlay1) -> {
                        PoseStack poseStack2 = new PoseStack();
                        poseStack2.last().normal().mul(normal);
                        poseStack2.last().pose().multiply(pose);
                        scopeRenderer.render(poseStack2, transformType1, light1, overlay1);
                    });
                }
            };
        });
        for (ModelRendererWrapper rendererWrapper : modelMap.values()) {
            if (rendererWrapper.getModelRenderer().name != null && rendererWrapper.getModelRenderer().name.endsWith("_illuminated")) {
                rendererWrapper.getModelRenderer().illuminated = true;
            }
        }
    }

    public void setScopeRenderer(@Nullable ISimpleRenderer renderer){
        scopeRenderer = renderer;
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

    @Nullable
    public List<BedrockPart> getConstraintPath(){
        return constraintPath;
    }

    @Nullable
    public List<BedrockPart> getRefitViewPath(){
        return refitViewPath;
    }

    public void setRenderHand(boolean renderHand){
        this.renderHand = renderHand;
    }

    public boolean getRenderHand(){
        return renderHand;
    }

    private void renderFirstPersonArm(LocalPlayer player, HumanoidArm hand, PoseStack matrixStack, int combinedLight) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        PlayerRenderer renderer = (PlayerRenderer) renderManager.getRenderer(player);
        MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
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
