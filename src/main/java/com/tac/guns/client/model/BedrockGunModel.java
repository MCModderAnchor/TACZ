package com.tac.guns.client.model;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.AnimationListener;
import com.tac.guns.client.animation.ObjectAnimationChannel;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.model.bedrock.ModelRendererWrapper;
import com.tac.guns.client.renderer.item.AttachmentItemRenderer;
import com.tac.guns.client.renderer.other.MuzzleFlashRender;
import com.tac.guns.client.renderer.other.ShellRender;
import com.tac.guns.client.resource.index.ClientAttachmentIndex;
import com.tac.guns.client.resource.index.ClientAttachmentSkinIndex;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import static com.tac.guns.client.model.CommonComponents.*;

public class BedrockGunModel extends BedrockAnimatedModel {
    private static final String IRON_VIEW_NODE = "iron_view";
    private static final String IDLE_VIEW_NODE = "idle_view";
    private static final String REFIT_VIEW_NODE = "refit_view";
    private static final String THIRD_PERSON_HAND_ORIGIN_NODE = "thirdperson_hand";
    private static final String FIXED_ORIGIN_NODE = "fixed";
    private static final String GROUND_ORIGIN_NODE = "ground";
    private static final String SHELL_ORIGIN_NODE = "shell";
    private static final String MUZZLE_FLASH_ORIGIN_NODE = "muzzle_flash";
    private static final String MAG_NORMAL_NODE = "magazine";
    private static final String MAG_ADDITIONAL_NODE = "additional_magazine";
    private static final String ATTACHMENT_POS_SUFFIX = "_pos";
    private static final String ATTACHMENT_ADAPTER_SUFFIX = "_adapter";
    private static final String REFIT_VIEW_PREFIX = "refit_";
    private static final String REFIT_VIEW_SUFFIX = "_view";
    private static final String ROOT_NODE = "root";
    protected final @Nonnull EnumMap<AttachmentType, List<BedrockPart>> refitAttachmentViewPath = Maps.newEnumMap(AttachmentType.class);
    private final EnumMap<AttachmentType, ItemStack> currentAttachmentItem = Maps.newEnumMap(AttachmentType.class);
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
    protected @Nullable BedrockPart root;
    protected @Nullable BedrockPart magazineNode;
    protected @Nullable BedrockPart additionalMagazineNode;
    private boolean renderHand = true;
    private ItemStack currentGunItem;
    // 枪口火焰模型
    private static final SlotModel MUZZLE_FLASH_MODEL = new SlotModel(true);
    private static Matrix3f muzzleFlashNormal = new Matrix3f();
    private static Matrix4f muzzleFlashPose = new Matrix4f();

    public BedrockGunModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        magazineNode = Optional.ofNullable(modelMap.get(MAG_NORMAL_NODE)).map(ModelRendererWrapper::getModelRenderer).orElse(null);
        additionalMagazineNode = Optional.ofNullable(modelMap.get(MAG_ADDITIONAL_NODE)).map(ModelRendererWrapper::getModelRenderer).orElse(null);
        this.setFunctionalRenderer("lefthand_pos", bedrockPart -> (poseStack, vertexBuffer, transformType, light, overlay) -> {
            if (transformType.firstPerson()) {
                if (!renderHand) {
                    return;
                }
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    RenderHelper.renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack2, light1);
                    Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
                });
            }
        });
        this.setFunctionalRenderer("righthand_pos", bedrockPart -> (poseStack, vertexBuffer, transformType, light, overlay) -> {
            if (transformType.firstPerson()) {
                if (!renderHand) {
                    return;
                }
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                Matrix3f normal = poseStack.last().normal().copy();
                Matrix4f pose = poseStack.last().pose().copy();
                //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
                this.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                    PoseStack poseStack2 = new PoseStack();
                    poseStack2.last().normal().mul(normal);
                    poseStack2.last().pose().multiply(pose);
                    RenderHelper.renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.RIGHT, poseStack2, light1);
                    Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
                });
            }
        });
        // 渲染枪口火焰
        this.setFunctionalRenderer(MUZZLE_FLASH_ORIGIN_NODE, bedrockPart -> (poseStack, vertexBuffer, transformType, light, overlay) -> {
            MuzzleFlashRender.render(currentGunItem, poseStack, this);
        });
        // 渲染抛壳
        this.setFunctionalRenderer(SHELL_ORIGIN_NODE, bedrockPart -> (poseStack, vertexBuffer, transformType, light, overlay) -> {
            ShellRender.render(currentGunItem, poseStack, this);
        });
        this.setFunctionalRenderer(BULLET_IN_BARREL, bedrockPart -> {
            // 弹药数大于 1 时渲染
            IGun iGun = IGun.getIGunOrNull(currentGunItem);
            if (iGun != null) {
                int ammoCount = iGun.getCurrentAmmoCount(currentGunItem);
                bedrockPart.visible = ammoCount > 0;
            }
            return null;
        });
        this.setFunctionalRenderer(BULLET_IN_MAG, bedrockPart -> {
            // 弹药数大于 1 时渲染
            IGun iGun = IGun.getIGunOrNull(currentGunItem);
            if (iGun != null) {
                int ammoCount = iGun.getCurrentAmmoCount(currentGunItem);
                bedrockPart.visible = ammoCount > 1;
            }
            return null;
        });
        this.setFunctionalRenderer(BULLET_CHAIN, bedrockPart -> {
            // 弹药数大于 1 时渲染
            IGun iGun = IGun.getIGunOrNull(currentGunItem);
            if (iGun != null) {
                int ammoCount = iGun.getCurrentAmmoCount(currentGunItem);
                bedrockPart.visible = ammoCount > 0;
            }
            return null;
        });
        this.setFunctionalRenderer(MOUNT, bedrockPart -> {
            // 安装瞄具时可见
            ItemStack scopeItem = currentAttachmentItem.get(AttachmentType.SCOPE);
            bedrockPart.visible = (scopeItem != null && !scopeItem.isEmpty());
            return null;
        });
        this.setFunctionalRenderer(CARRY, bedrockPart -> {
            // 未安装瞄具时可见
            ItemStack scopeItem = currentAttachmentItem.get(AttachmentType.SCOPE);
            bedrockPart.visible = (scopeItem == null || scopeItem.isEmpty());
            return null;
        });
        this.setFunctionalRenderer(SIGHT_FOLDED, bedrockPart -> {
            // 安装瞄具时可见
            ItemStack scopeItem = currentAttachmentItem.get(AttachmentType.SCOPE);
            bedrockPart.visible = (scopeItem != null && !scopeItem.isEmpty());
            return null;
        });
        this.setFunctionalRenderer(SIGHT, bedrockPart -> {
            // 未安装瞄具时可见
            ItemStack scopeItem = currentAttachmentItem.get(AttachmentType.SCOPE);
            bedrockPart.visible = (scopeItem == null || scopeItem.isEmpty());
            return null;
        });
        this.setFunctionalRenderer(MAG_EXTENDED_1, bedrockPart -> getExtendedMagLevel(bedrockPart, 1));
        this.setFunctionalRenderer(MAG_EXTENDED_2, bedrockPart -> getExtendedMagLevel(bedrockPart, 2));
        this.setFunctionalRenderer(MAG_EXTENDED_3, bedrockPart -> getExtendedMagLevel(bedrockPart, 3));
        this.setFunctionalRenderer(MAG_STANDARD, bedrockPart -> {
            ItemStack extendedMagItem = currentAttachmentItem.get(AttachmentType.EXTENDED_MAG);
            if (extendedMagItem == null || extendedMagItem.isEmpty()) {
                bedrockPart.visible = true;
                return null;
            }
            IAttachment attachment = IAttachment.getIAttachmentOrNull(extendedMagItem);
            if (attachment == null) {
                bedrockPart.visible = true;
                return null;
            }
            TimelessAPI.getCommonAttachmentIndex(attachment.getAttachmentId(extendedMagItem)).ifPresent(index -> {
                bedrockPart.visible = (index.getData().getExtendedMagLevel() <= 0 || index.getData().getExtendedMagLevel() > 3);
            });
            return null;
        });
        this.setFunctionalRenderer(MAG_ADDITIONAL_NODE, bedrockPart -> (poseStack, vertexBuffer, transformType, light, overlay) -> {
            if (bedrockPart.visible) {
                bedrockPart.compile(poseStack.last(), vertexBuffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                for (BedrockPart part : bedrockPart.children) {
                    part.render(poseStack, transformType, vertexBuffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                if (magazineNode != null && magazineNode.visible) {
                    magazineNode.compile(poseStack.last(), vertexBuffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    for (BedrockPart part : magazineNode.children) {
                        part.render(poseStack, transformType, vertexBuffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        });

        ironSightPath = getPath(modelMap.get(IRON_VIEW_NODE));
        idleSightPath = getPath(modelMap.get(IDLE_VIEW_NODE));
        thirdPersonHandOriginPath = getPath(modelMap.get(THIRD_PERSON_HAND_ORIGIN_NODE));
        fixedOriginPath = getPath(modelMap.get(FIXED_ORIGIN_NODE));
        groundOriginPath = getPath(modelMap.get(GROUND_ORIGIN_NODE));
        scopePosPath = getPath(modelMap.get(AttachmentType.SCOPE.name().toLowerCase() + ATTACHMENT_POS_SUFFIX));
        ModelRendererWrapper rootWrapper = modelMap.get(ROOT_NODE);
        if (rootWrapper != null) {
            root = rootWrapper.getModelRenderer();
        }

        // 缓存改装UI下各个配件的特写视角定位组
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                refitAttachmentViewPath.put(type, getPath(modelMap.get(REFIT_VIEW_NODE)));
                continue;
            }
            String nodeName = REFIT_VIEW_PREFIX + type.name().toLowerCase() + REFIT_VIEW_SUFFIX;
            refitAttachmentViewPath.put(type, getPath(modelMap.get(nodeName)));
        }

        // 准备各个配件的渲染
        for (AttachmentType type : AttachmentType.values()) {
            // 瞄具的渲染需要提前
            if (type == AttachmentType.NONE || type == AttachmentType.SCOPE) {
                continue;
            }
            String nodeName = type.name().toLowerCase() + ATTACHMENT_POS_SUFFIX;
            this.setFunctionalRenderer(nodeName, bedrockPart -> {
                bedrockPart.visible = false;
                return (poseStack, vertexBuffer, transformType, light, overlay) -> {
                    ItemStack attachmentItem = currentAttachmentItem.get(type);
                    if (attachmentItem != null && !attachmentItem.isEmpty()) {
                        Matrix3f normal = poseStack.last().normal().copy();
                        Matrix4f pose = poseStack.last().pose().copy();
                        //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
                        this.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                            PoseStack poseStack2 = new PoseStack();
                            poseStack2.last().normal().mul(normal);
                            poseStack2.last().pose().multiply(pose);
                            // 渲染配件
                            renderAttachment(attachmentItem, poseStack2, transformType, light, overlay);
                        });
                    }
                };
            });
        }
    }

    @Nullable
    private IFunctionalRenderer getExtendedMagLevel(BedrockPart bedrockPart, int level) {
        ItemStack extendedMagItem = currentAttachmentItem.get(AttachmentType.EXTENDED_MAG);
        if (extendedMagItem == null || extendedMagItem.isEmpty()) {
            bedrockPart.visible = false;
            return null;
        }
        IAttachment attachment = IAttachment.getIAttachmentOrNull(extendedMagItem);
        if (attachment == null) {
            bedrockPart.visible = false;
            return null;
        }
        TimelessAPI.getCommonAttachmentIndex(attachment.getAttachmentId(extendedMagItem)).ifPresent(index -> {
            bedrockPart.visible = (index.getData().getExtendedMagLevel() == level);
        });
        return null;
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
    public List<BedrockPart> getRefitAttachmentViewPath(AttachmentType type) {
        return refitAttachmentViewPath.get(type);
    }

    @Nullable
    public BedrockPart getRootNode() {
        return root;
    }

    public boolean getRenderHand() {
        return renderHand;
    }

    public void setRenderHand(boolean renderHand) {
        this.renderHand = renderHand;
    }

    public void render(PoseStack matrixStack, ItemStack gunItem, ItemTransforms.TransformType transformType, RenderType renderType, int light, int overlay) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        currentGunItem = gunItem;
        // 更新配件物品的缓存，以供渲染使用
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            ItemStack attachmentItem = iGun.getAttachment(gunItem, type);
            currentAttachmentItem.put(type, attachmentItem);
        }
        // 镜子需要先渲染，写入模板值
        ItemStack attachmentItem = currentAttachmentItem.get(AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (scopePosPath != null && attachmentItem != null && !attachmentItem.isEmpty()) {
            matrixStack.pushPose();
            for (BedrockPart bedrockPart : scopePosPath) {
                bedrockPart.translateAndRotateAndScale(matrixStack);
            }
            renderAttachment(attachmentItem, matrixStack, transformType, light, overlay);
            matrixStack.popPose();
            // 开启模板测试，因为镜内不渲染枪体
            if (iAttachment != null) {
                Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(attachmentItem));
                attachmentIndex.ifPresent(index -> {
                    if (index.isScope()) {
                        RenderHelper.enableItemEntityStencilTest();
                    }
                });
            }
        }
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        super.render(matrixStack, transformType, renderType, light, overlay);
        RenderHelper.disableItemEntityStencilTest();
    }

    @Override
    public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
        AnimationListener listener = super.supplyListeners(nodeName, type);
        if (listener == null) {
            return null;
        }
        if (nodeName.equals(MAG_ADDITIONAL_NODE)) {
            // 额外弹匣只有当动画中有它的关键帧的时候才渲染
            return new AnimationListener() {
                @Override
                public void update(float[] values, boolean blend) {
                    listener.update(values, blend);
                    if (additionalMagazineNode != null) {
                        additionalMagazineNode.visible = true;
                    }
                }
                @Override
                public float[] recover() {
                    return listener.recover();
                }
                @Override
                public ObjectAnimationChannel.ChannelType getType() {
                    return listener.getType();
                }
            };
        }
        return listener;
    }

    @Override
    public void cleanAnimationTransform() {
        super.cleanAnimationTransform();
        if (additionalMagazineNode != null) {
            additionalMagazineNode.visible = false;
        }
    }

    private void renderAttachment(ItemStack attachmentItem, PoseStack poseStack, ItemTransforms.TransformType transformType, int light, int overlay) {
        poseStack.translate(0, -1.5, 0);
        if (attachmentItem.getItem() instanceof IAttachment iAttachment) {
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresentOrElse(attachmentIndex -> {
                ResourceLocation skinId = iAttachment.getSkinId(attachmentItem);
                ClientAttachmentSkinIndex skinIndex = attachmentIndex.getSkinIndex(skinId);
                if (skinIndex != null) {
                    // 有皮肤则渲染皮肤
                    BedrockAttachmentModel model = skinIndex.getModel();
                    ResourceLocation texture = skinIndex.getTexture();
                    RenderType renderType = RenderType.itemEntityTranslucentCull(texture);
                    model.render(poseStack, transformType, renderType, light, overlay);
                } else {
                    // 没有皮肤，渲染默认模型
                    BedrockAttachmentModel model = attachmentIndex.getAttachmentModel();
                    ResourceLocation texture = attachmentIndex.getModelTexture();
                    RenderType renderType = RenderType.itemEntityTranslucentCull(texture);
                    model.render(poseStack, transformType, renderType, light, overlay);
                }
            }, () -> {
                // 没有对应的 attachmentIndex，渲染黑紫材质以提醒
                MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                AttachmentItemRenderer.SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            });
        }
    }
}
