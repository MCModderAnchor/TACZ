package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.client.animation.AnimationListener;
import com.tac.guns.client.animation.AnimationListenerSupplier;
import com.tac.guns.client.animation.ObjectAnimationChannel;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.model.bedrock.ModelRendererWrapper;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.client.resource.pojo.model.BonesItem;
import com.tac.guns.util.math.MathUtil;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class BedrockAnimatedModel extends BedrockModel implements AnimationListenerSupplier {
    public static final String CAMERA_NODE_NAME = "camera";
    private static final String CONSTRAINT_NODE = "constraint";
    private final CameraAnimationObject cameraAnimationObject = new CameraAnimationObject();
    // 动画约束组的路径
    protected @Nullable List<BedrockPart> constraintPath;
    @Nullable
    private ConstraintObject constraintObject;

    public BedrockAnimatedModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        // 初始化相机动画对象
        ModelRendererWrapper cameraRendererWrapper = modelMap.get(CAMERA_NODE_NAME);
        if (cameraRendererWrapper != null) {
            cameraAnimationObject.cameraRenderer = cameraRendererWrapper;
        }
        // 初始化动画约束对象
        constraintPath = getPath(modelMap.get(CONSTRAINT_NODE));
        if (constraintPath != null) {
            constraintObject = new ConstraintObject();
            BedrockPart constraintNode = constraintPath.get(constraintPath.size() - 1);
            if (shouldRender.contains(constraintNode)) {
                constraintObject.bonesItem = indexBones.get(CONSTRAINT_NODE);
            } else {
                constraintObject.node = constraintNode;
            }
        }
    }

    private static void toQuaternion(float roll, float pitch, float yaw, @Nonnull Quaternion quaternion) {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        quaternion.set(
                (float) (cy * cp * sr - sy * sp * cr),
                (float) (sy * cp * sr + cy * sp * cr),
                (float) (sy * cp * cr - cy * sp * sr),
                (float) (cy * cp * cr + sy * sp * sr)
        );
    }

    @Nullable
    public List<BedrockPart> getConstraintPath() {
        return constraintPath;
    }

    public void setVisible(String bone, boolean visible) {
        ModelRendererWrapper rendererWrapper = modelMap.get(bone);
        if (rendererWrapper != null) {
            rendererWrapper.setHidden(visible);
        }
    }

    public void cleanAnimationTransform() {
        for (ModelRendererWrapper rendererWrapper : modelMap.values()) {
            rendererWrapper.setOffsetX(0);
            rendererWrapper.setOffsetY(0);
            rendererWrapper.setOffsetZ(0);
            rendererWrapper.getAdditionalQuaternion().set(0, 0, 0, 1);
            rendererWrapper.setScaleX(1);
            rendererWrapper.setScaleY(1);
            rendererWrapper.setScaleZ(1);
        }
        if (constraintObject != null) {
            constraintObject.rotationConstraint.set(0, 0, 0);
            constraintObject.translationConstraint.set(0, 0, 0);
        }
    }

    public void cleanCameraAnimationTransform() {
        cameraAnimationObject.rotationQuaternion = Quaternion.ONE.copy();
    }

    /**
     * @param node     想要进行编程渲染流程的 node 名称
     * @param function 输入为 BedrockPart，返回 IModelRenderer 以替换渲染
     */
    public void setFunctionalRenderer(String node, Function<BedrockPart, IFunctionalRenderer> function) {
        ModelRendererWrapper wrapper = modelMap.get(node);
        if (wrapper == null) {
            FunctionalBedrockPart functionalPart = new FunctionalBedrockPart(function, node);
            modelMap.put(node, new ModelRendererWrapper(functionalPart));
        } else if (wrapper.getModelRenderer() instanceof FunctionalBedrockPart functionalPart) {
            functionalPart.functionalRenderer = function;
        }
    }

    public @Nonnull CameraAnimationObject getCameraAnimationObject() {
        return cameraAnimationObject;
    }

    public @Nullable ConstraintObject getConstraintObject() {
        return constraintObject;
    }

    @Override
    protected void loadNewModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelNew() != null;
        pojo.getGeometryModelNew().deco();
        if (pojo.getGeometryModelNew().getBones() == null) {
            return;
        }
        for (BonesItem bones : pojo.getGeometryModelNew().getBones()) {
            // 将 FunctionalBedrockPart 先塞入 modelMap 中，以支持 functionalRender 操作
            modelMap.putIfAbsent(bones.getName(), new ModelRendererWrapper(new FunctionalBedrockPart(null, bones.getName())));
        }
        super.loadNewModel(pojo);
    }

    @Override
    protected void loadLegacyModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelLegacy() != null;
        pojo.getGeometryModelLegacy().deco();
        if (pojo.getGeometryModelLegacy().getBones() == null) {
            return;
        }
        for (BonesItem bones : pojo.getGeometryModelLegacy().getBones()) {
            // 将 FunctionalBedrockPart 先塞入 modelMap 中，以支持 functionalRender 操作
            modelMap.putIfAbsent(bones.getName(), new ModelRendererWrapper(new FunctionalBedrockPart(null, bones.getName())));
        }
        super.loadLegacyModel(pojo);
    }

    @Override
    public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
        ModelRendererWrapper model = modelMap.get(nodeName);
        if (model == null) {
            return null;
        }

        AnimationListener cameraListener = cameraAnimationObject.supplyListeners(nodeName, type);
        if (cameraListener != null) {
            return cameraListener;
        }

        if (constraintObject != null) {
            AnimationListener constraintListener = constraintObject.supplyListeners(nodeName, type);
            if (constraintListener != null) {
                return constraintListener;
            }
        }

        if (type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
            return new AnimationListener() {
                final ModelRendererWrapper rendererWrapper = model;
                BonesItem bonesItem;

                {
                    //如果当前node是根node(也就是包含于shouldRender中)，则获取其bonesItem，以便后续计算相对位移 offset。
                    if (shouldRender.contains(rendererWrapper.getModelRenderer())) {
                        bonesItem = indexBones.get(nodeName);
                    }
                }

                @Override
                public void update(float[] values, boolean blend) {
                    if (bonesItem != null) {
                        // 因为模型是上下颠倒的，因此此处x轴和y轴的偏移也进行取反
                        // 因为要达成所有位移都是相对位移，所以如果当前node是根node，则减去根node的pivot坐标。
                        if (blend) { // 约束组动画是特殊值，不参与混合
                            rendererWrapper.addOffsetX(-values[0] - bonesItem.getPivot().get(0) / 16f);
                            rendererWrapper.addOffsetY(-values[1] + bonesItem.getPivot().get(1) / 16f);
                            rendererWrapper.addOffsetZ(values[2] - bonesItem.getPivot().get(2) / 16f);
                        } else {
                            rendererWrapper.setOffsetX(-values[0] - bonesItem.getPivot().get(0) / 16f);
                            rendererWrapper.setOffsetY(-values[1] + bonesItem.getPivot().get(1) / 16f);
                            rendererWrapper.setOffsetZ(values[2] - bonesItem.getPivot().get(2) / 16f);
                        }
                    } else {
                        // 因为模型是上下颠倒的，因此此处x轴和y轴的偏移也进行取反
                        // 虽然方法名称写的是getRotationPoint，但其实还是相对父级node的坐标移动量。因此此处与listener提供的local translation相减。
                        if (blend) { // 约束组动画是特殊值，不参与混合
                            rendererWrapper.addOffsetX(-values[0] - rendererWrapper.getRotationPointX() / 16f);
                            rendererWrapper.addOffsetY(-values[1] - rendererWrapper.getRotationPointY() / 16f);
                            rendererWrapper.addOffsetZ(values[2] - rendererWrapper.getRotationPointZ() / 16f);
                        } else {
                            rendererWrapper.setOffsetX(-values[0] - rendererWrapper.getRotationPointX() / 16f);
                            rendererWrapper.setOffsetY(-values[1] - rendererWrapper.getRotationPointY() / 16f);
                            rendererWrapper.setOffsetZ(values[2] - rendererWrapper.getRotationPointZ() / 16f);
                        }
                    }
                }

                @Override
                public float[] recover() { // 目标是让 offset 过渡为 0.
                    float[] recover = new float[3];
                    if (bonesItem != null) {
                        recover[0] = -bonesItem.getPivot().get(0) / 16f;
                        recover[1] = bonesItem.getPivot().get(1) / 16f;
                        recover[2] = bonesItem.getPivot().get(2) / 16f;
                    } else {
                        recover[0] = -rendererWrapper.getRotationPointX() / 16f;
                        recover[1] = -rendererWrapper.getRotationPointY() / 16f;
                        recover[2] = rendererWrapper.getRotationPointZ() / 16f;
                    }
                    return recover;
                }

                @Override
                public ObjectAnimationChannel.ChannelType getType() {
                    return ObjectAnimationChannel.ChannelType.TRANSLATION;
                }
            };
        }

        if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
            return new AnimationListener() {
                final ModelRendererWrapper rendererWrapper = model;

                @Override
                public void update(float[] values, boolean blend) {
                    float[] angles = MathUtil.toEulerAngles(values);
                    // 计算 roll（绕 x 轴的旋转角）
                    float roll = angles[0];
                    // 计算 pitch（绕 y 轴的旋转角）
                    float pitch = angles[1];
                    // 计算 yaw（绕 z 轴的旋转角）
                    float yaw = angles[2];
                    // 因为模型是上下颠倒的，因此此处 yaw 轴的旋转需要进行取反
                    // 要减去模型组的初始旋转值，写入相对值。
                    if (blend) { // 约束组动画是特殊值，不参与混合
                        float[] q = MathUtil.toQuaternion(
                                -roll - rendererWrapper.getRotateAngleX(),
                                -pitch - rendererWrapper.getRotateAngleY(),
                                yaw - rendererWrapper.getRotateAngleZ()
                        );
                        Quaternion quaternion = MathUtil.toQuaternion(q);
                        MathUtil.blendQuaternion(rendererWrapper.getAdditionalQuaternion(), quaternion);
                    } else {
                        toQuaternion(
                                -roll - rendererWrapper.getRotateAngleX(),
                                -pitch - rendererWrapper.getRotateAngleY(),
                                yaw - rendererWrapper.getRotateAngleZ(),
                                rendererWrapper.getAdditionalQuaternion()
                        );
                    }
                }

                @Override
                public float[] recover() { // 目标是让相对旋转为 0.
                    return MathUtil.toQuaternion(-rendererWrapper.getRotateAngleX(), rendererWrapper.getRotateAngleY(), rendererWrapper.getRotateAngleZ());
                }

                @Override
                public ObjectAnimationChannel.ChannelType getType() {
                    return ObjectAnimationChannel.ChannelType.ROTATION;
                }
            };
        }

        if (type.equals(ObjectAnimationChannel.ChannelType.SCALE)) {
            return new AnimationListener() {
                final ModelRendererWrapper rendererWrapper = model;

                @Override
                public void update(float[] values, boolean blend) {
                    if (blend) {
                        rendererWrapper.setScaleX(rendererWrapper.getScaleX() * values[0]);
                        rendererWrapper.setScaleY(rendererWrapper.getScaleY() * values[1]);
                        rendererWrapper.setScaleZ(rendererWrapper.getScaleZ() * values[2]);
                    } else {
                        rendererWrapper.setScaleX(values[0]);
                        rendererWrapper.setScaleY(values[1]);
                        rendererWrapper.setScaleZ(values[2]);
                    }
                }

                @Override
                public float[] recover() {
                    return new float[]{1f, 1f, 1f};
                }

                @Override
                public ObjectAnimationChannel.ChannelType getType() {
                    return ObjectAnimationChannel.ChannelType.SCALE;
                }
            };
        }

        return null;
    }

    /**
     * visible的优先级低于FunctionalBedrockPart，当visible为false的时候，仍然会执行functionalRenderers
     */
    protected static class FunctionalBedrockPart extends BedrockPart {
        public @Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer;

        public FunctionalBedrockPart(@Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer, @Nonnull String name) {
            super(name);
            this.functionalRenderer = functionalRenderer;
        }

        public FunctionalBedrockPart(@Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer, @Nonnull BedrockPart part) {
            super(part.name);
            this.cubes.addAll(part.cubes);
            this.children.addAll(part.children);
            this.x = part.x;
            this.y = part.y;
            this.z = part.z;
            this.xRot = part.xRot;
            this.yRot = part.yRot;
            this.zRot = part.zRot;
            this.offsetX = part.offsetX;
            this.offsetY = part.offsetY;
            this.offsetZ = part.offsetZ;
            this.visible = part.visible;
            this.mirror = part.mirror;
            this.setInitRotationAngle(part.getInitRotX(), part.getInitRotY(), part.getInitRotZ());
            this.xScale = part.xScale;
            this.yScale = part.yScale;
            this.zScale = part.zScale;
            this.functionalRenderer = functionalRenderer;
        }

        @Override
        public void render(PoseStack poseStack, ItemTransforms.TransformType transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
            int cubePackedLight = light;
            if (illuminated) {
                // 最大亮度
                cubePackedLight = LightTexture.pack(15, 15);
            }

            poseStack.pushPose();
            this.translateAndRotateAndScale(poseStack);

            if (functionalRenderer != null) {
                @Nullable IFunctionalRenderer renderer = functionalRenderer.apply(this);
                if (renderer != null) {
                    renderer.render(poseStack, consumer, transformType, cubePackedLight, overlay);
                } else {
                    if (this.visible) {
                        super.compile(poseStack.last(), consumer, cubePackedLight, overlay, red, green, blue, alpha);
                        for (BedrockPart part : this.children) {
                            part.render(poseStack, transformType, consumer, cubePackedLight, overlay, red, green, blue, alpha);
                        }
                    }
                }
            } else {
                if (this.visible) {
                    super.compile(poseStack.last(), consumer, cubePackedLight, overlay, red, green, blue, alpha);
                    for (BedrockPart part : this.children) {
                        part.render(poseStack, transformType, consumer, cubePackedLight, overlay, red, green, blue, alpha);
                    }
                }
            }
            poseStack.popPose();
        }
    }

    public static class ConstraintObject implements AnimationListenerSupplier {
        public Vector3f translationConstraint = new Vector3f(0, 0, 0);
        public Vector3f rotationConstraint = new Vector3f(0, 0, 0);
        /**
         * 当相机的节点为根时，node为空
         */
        protected BedrockPart node;
        /**
         * 当相机的节点不为根时，bonesItem为空
         */
        protected BonesItem bonesItem;

        @Nullable
        @Override
        public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
            if (!nodeName.equals(CONSTRAINT_NODE)) {
                return null;
            }
            if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
                return new AnimationListener() {
                    @Override
                    public void update(float[] values, boolean blend) {
                        float[] angles = MathUtil.toEulerAngles(values);
                        if (blend) {
                            rotationConstraint.set(
                                    (float) Math.max(rotationConstraint.x(), MathUtil.toDegreePositive(-angles[0])),
                                    (float) Math.max(rotationConstraint.y(), MathUtil.toDegreePositive(-angles[1])),
                                    (float) Math.max(rotationConstraint.z(), MathUtil.toDegreePositive(angles[2]))
                            );
                        } else {
                            rotationConstraint.set((float) MathUtil.toDegreePositive(-angles[0]), (float) MathUtil.toDegreePositive(-angles[1]), (float) MathUtil.toDegreePositive(angles[2]));
                        }
                    }

                    @Override
                    public float[] recover() {
                        return new float[]{0, 0, 0, 1};
                    }

                    @Override
                    public ObjectAnimationChannel.ChannelType getType() {
                        return ObjectAnimationChannel.ChannelType.ROTATION;
                    }
                };
            }
            if (type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
                return new AnimationListener() {
                    @Override
                    public void update(float[] values, boolean blend) {
                        if (bonesItem != null) {
                            if (blend) {
                                translationConstraint.set(
                                        Math.max(translationConstraint.x(), -values[0] * 16 - bonesItem.getPivot().get(0)),
                                        Math.max(translationConstraint.y(), values[1] * 16 - bonesItem.getPivot().get(1)),
                                        Math.max(translationConstraint.z(), values[2] * 16 - bonesItem.getPivot().get(2))
                                );
                            } else {
                                translationConstraint.set(
                                        -values[0] * 16 - bonesItem.getPivot().get(0),
                                        values[1] * 16 - bonesItem.getPivot().get(1),
                                        values[2] * 16 - bonesItem.getPivot().get(2)
                                );
                            }
                        } else {
                            if (blend) {
                                translationConstraint.set(
                                        Math.max(translationConstraint.x(), -values[0] * 16 - node.x),
                                        Math.max(translationConstraint.y(), values[1] * 16 + node.y),
                                        Math.max(translationConstraint.z(), values[2] * 16 - node.z)
                                );
                            } else {
                                translationConstraint.set(
                                        -values[0] * 16 - node.x,
                                        values[1] * 16 + node.y,
                                        values[2] * 16 - node.z
                                );
                            }
                        }
                    }

                    @Override
                    public float[] recover() {
                        float[] recover = new float[3];
                        if (bonesItem != null) {
                            recover[0] = -bonesItem.getPivot().get(0) / 16f;
                            recover[1] = bonesItem.getPivot().get(1) / 16f;
                            recover[2] = bonesItem.getPivot().get(2) / 16f;
                        } else {
                            recover[0] = -node.x / 16f;
                            recover[1] = -node.y / 16f;
                            recover[2] = node.z / 16f;
                        }
                        return recover;
                    }

                    @Override
                    public ObjectAnimationChannel.ChannelType getType() {
                        return ObjectAnimationChannel.ChannelType.TRANSLATION;
                    }
                };
            }
            return null;
        }
    }

    public static class CameraAnimationObject implements AnimationListenerSupplier {
        /**
         * 存在这个四元数中的旋转是世界箱体的旋转，而不是摄像头的旋转（二者互为相反数）
         */
        public Quaternion rotationQuaternion = Quaternion.ONE.copy();

        /**
         * 当相机的节点为根时，cameraRenderer为空
         */
        protected ModelRendererWrapper cameraRenderer;

        @Override
        public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
            if (!nodeName.equals(CAMERA_NODE_NAME)) {
                return null;
            }
            if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
                return new AnimationListener() {
                    @Override
                    public void update(float[] values, boolean blend) {
                        float[] angles = MathUtil.toEulerAngles(values);
                        // 计算 roll（绕 x 轴的旋转角）
                        float roll = angles[0];
                        // 计算 pitch（绕 y 轴的旋转角）
                        float pitch = angles[1];
                        // 计算 yaw（绕 z 轴的旋转角）
                        float yaw = angles[2];
                        /*
                        在关键帧中储存的旋转数值并不是摄像头的旋转数值，是世界箱体的旋转数值
                        最终需要存入rotationQuaternion的是摄像机的旋转（即世界箱体旋转的反相）
                        */
                        if (blend) {
                            float[] q = MathUtil.toQuaternion(
                                    -roll,
                                    -pitch,
                                    -yaw
                            );
                            Quaternion quaternion = MathUtil.toQuaternion(q);
                            MathUtil.blendQuaternion(rotationQuaternion, quaternion);
                        } else {
                            toQuaternion(
                                    -roll,
                                    -pitch,
                                    -yaw,
                                    rotationQuaternion
                            );
                        }
                    }

                    @Override
                    public float[] recover() {
                        return MathUtil.toQuaternion(-cameraRenderer.getRotateAngleX(), cameraRenderer.getRotateAngleY(), cameraRenderer.getRotateAngleZ());
                    }

                    @Override
                    public ObjectAnimationChannel.ChannelType getType() {
                        return ObjectAnimationChannel.ChannelType.ROTATION;
                    }
                };
            }
            return null;
        }
    }
}
