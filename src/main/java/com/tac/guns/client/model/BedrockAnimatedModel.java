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
import java.util.function.Function;

public class BedrockAnimatedModel extends BedrockModel implements AnimationListenerSupplier {
    private final CameraAnimationObject cameraAnimationObject = new CameraAnimationObject();

    public BedrockAnimatedModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        // 初始化相机动画对象
        ModelRendererWrapper cameraRendererWrapper = modelMap.get(CameraAnimationObject.CAMERA_NODE_NAME);
        if (cameraRendererWrapper != null) {
            if (shouldRender.contains(cameraRendererWrapper.getModelRenderer())) {
                cameraAnimationObject.cameraBone = indexBones.get(CameraAnimationObject.CAMERA_NODE_NAME);
            }
            cameraAnimationObject.cameraRenderer = cameraRendererWrapper;
        }
    }

    private static float dot(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
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

    private static void quaternionToMatrix4x4(float[] q, float[] m) {
        float invLength = 1.0f / (float) Math.sqrt(dot(q, q));
        float qx = q[0] * invLength;
        float qy = q[1] * invLength;
        float qz = q[2] * invLength;
        float qw = q[3] * invLength;
        m[0] = 1.0f - 2.0f * qy * qy - 2.0f * qz * qz;
        m[1] = 2.0f * (qx * qy + qw * qz);
        m[2] = 2.0f * (qx * qz - qw * qy);
        m[3] = 0.0f;
        m[4] = 2.0f * (qx * qy - qw * qz);
        m[5] = 1.0f - 2.0f * qx * qx - 2.0f * qz * qz;
        m[6] = 2.0f * (qy * qz + qw * qx);
        m[7] = 0.0f;
        m[8] = 2.0f * (qx * qz + qw * qy);
        m[9] = 2.0f * (qy * qz - qw * qx);
        m[10] = 1.0f - 2.0f * qx * qx - 2.0f * qy * qy;
        m[11] = 0.0f;
        m[12] = 0.0f;
        m[13] = 0.0f;
        m[14] = 0.0f;
        m[15] = 1.0f;
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
    }

    /**
     * @param node     想要进行编程渲染流程的 node 名称
     * @param function 输入为 BedrockPart，返回 IModelRenderer 以替换渲染
     */
    public void setFunctionalRenderer(String node, Function<BedrockPart, IModelRenderer> function) {
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

    @Override
    protected void loadNewModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelNew() != null;
        pojo.getGeometryModelNew().deco();
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
                public void update(float[] values) {
                    // 因为模型是上下颠倒的，因此此处x轴和y轴的偏移也进行取反
                    if (bonesItem != null) {
                        // 因为要达成所有位移都是相对位移，所以如果当前node是根node，则减去根node的pivot坐标。
                        rendererWrapper.setOffsetX(-values[0] - bonesItem.getPivot().get(0) / 16f);
                        rendererWrapper.setOffsetY(-values[1] + bonesItem.getPivot().get(1) / 16f);
                        rendererWrapper.setOffsetZ(values[2] - bonesItem.getPivot().get(2) / 16f);
                    } else {
                        // 虽然方法名称写的是getRotationPoint，但其实还是相对父级node的坐标移动量。因此此处与listener提供的local translation相减。
                        rendererWrapper.setOffsetX(-values[0] - rendererWrapper.getRotationPointX() / 16f);
                        rendererWrapper.setOffsetY(-values[1] - rendererWrapper.getRotationPointY() / 16f);
                        rendererWrapper.setOffsetZ(values[2] - rendererWrapper.getRotationPointZ() / 16f);
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
                public void update(float[] values) {
                    float[] m = new float[16];
                    quaternionToMatrix4x4(values, m);
                    // 计算 roll（绕 x 轴的旋转角）
                    float roll = (float) Math.atan2(m[6], m[10]);
                    // 计算 pitch（绕 y 轴的旋转角）
                    float pitch = (float) Math.atan2(m[2], Math.sqrt(m[6] * m[6] + m[10] * m[10]));
                    // 计算 yaw（绕 z 轴的旋转角）
                    float yaw = (float) Math.atan2(m[1], m[0]);
                    // 因为模型是上下颠倒的，因此此处roll轴的旋转需要进行取反
                    // 要减去模型组的初始旋转值，写入相对值。
                    toQuaternion(
                            -roll - rendererWrapper.getRotateAngleX(),
                            pitch - rendererWrapper.getRotateAngleY(),
                            yaw - rendererWrapper.getRotateAngleZ(),
                            rendererWrapper.getAdditionalQuaternion()
                    );
                }

                @Override
                public float[] recover() {
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
                public void update(float[] values) {
                    rendererWrapper.setScaleX(values[0]);
                    rendererWrapper.setScaleY(values[1]);
                    rendererWrapper.setScaleZ(values[2]);
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
        public @Nullable Function<BedrockPart, IModelRenderer> functionalRenderer;

        public FunctionalBedrockPart(@Nullable Function<BedrockPart, IModelRenderer> functionalRenderer, @Nonnull String name) {
            super(name);
            this.functionalRenderer = functionalRenderer;
        }

        public FunctionalBedrockPart(@Nullable Function<BedrockPart, IModelRenderer> functionalRenderer, @Nonnull BedrockPart part) {
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
            poseStack.translate(this.offsetX, this.offsetY, this.offsetZ);
            this.translateAndRotateAndScale(poseStack);

            if (functionalRenderer != null) {
                @Nullable IModelRenderer renderer = functionalRenderer.apply(this);
                if (renderer != null) {
                    renderer.render(poseStack, transformType, consumer, cubePackedLight, overlay);
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

    public static class CameraAnimationObject implements AnimationListenerSupplier {
        public static final String CAMERA_NODE_NAME = "camera";
        /**
         * 存在这个四元数中的旋转是世界箱体的旋转，而不是摄像头的旋转（二者互为相反数）
         */
        public Quaternion rotationQuaternion = Quaternion.ONE.copy();
        public Vector3f translationVector = new Vector3f();

        /**
         * 当相机的节点为根时，cameraRenderer为空
         */
        protected ModelRendererWrapper cameraRenderer;
        /**
         * 当相机的节点不为根时，cameraBone为空
         */
        protected BonesItem cameraBone;

        @Override
        public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
            if (!nodeName.equals(CAMERA_NODE_NAME)) {
                return null;
            }
            if (type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
                return new AnimationListener() {
                    @Override
                    public void update(float[] values) {
                        if (cameraBone != null) {
                            //因为要达成所有位移都是相对位移，所以如果当前node是根node，则减去根node的pivot坐标。
                            translationVector.setX(values[0] - cameraBone.getPivot().get(0) / 16f);
                            translationVector.setY(values[1] - cameraBone.getPivot().get(1) / 16f);
                            translationVector.setZ(values[2] - cameraBone.getPivot().get(2) / 16f);
                        } else {
                            //虽然方法名称写的是getRotationPoint，但其实还是相对父级node的坐标移动量。因此此处与listener提供的local translation相减。
                            translationVector.setX(values[0] + cameraRenderer.getRotationPointX() / 16f);
                            translationVector.setY(values[1] + cameraRenderer.getRotationPointY() / 16f);
                            translationVector.setZ(values[2] - cameraRenderer.getRotationPointZ() / 16f);
                        }
                    }

                    @Override
                    public float[] recover() {
                        float[] recover = new float[3];
                        if (cameraBone != null) {
                            recover[0] = cameraBone.getPivot().get(0) / 16f;
                            recover[1] = cameraBone.getPivot().get(1) / 16f;
                            recover[2] = cameraBone.getPivot().get(2) / 16f;
                        } else {
                            recover[0] = -cameraRenderer.getRotationPointX() / 16f;
                            recover[1] = -cameraRenderer.getRotationPointY() / 16f;
                            recover[2] = cameraRenderer.getRotationPointZ() / 16f;
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
                    @Override
                    public void update(float[] values) {
                        float[] m = new float[16];
                        quaternionToMatrix4x4(values, m);
                        // 计算 roll（绕 x 轴的旋转角）
                        float roll = (float) Math.atan2(m[6], m[10]);
                        // 计算 pitch（绕 y 轴的旋转角）
                        float pitch = (float) Math.atan2(m[2], Math.sqrt(m[6] * m[6] + m[10] * m[10]));
                        // 计算 roll（绕 z 轴的旋转角）
                        float yaw = (float) Math.atan2(m[1], m[0]);
                        /*
                        对roll和yaw取反单纯是因为需要使用 blockbench 的camera插件，
                        它在关键帧中储存的旋转数值并不是摄像头的旋转数值，是世界箱体的旋转数值，
                        但唯独pitch是反的(也就是说唯独pitch是摄像机的旋转数值)。
                        最终需要存入rotationQuaternion的是世界箱体的旋转，因此roll yaw取反，pitch不需要
                        */
                        toQuaternion(
                                -roll - cameraRenderer.getRotateAngleX(),
                                pitch - cameraRenderer.getRotateAngleY(),
                                -yaw + cameraRenderer.getRotateAngleZ(),
                                rotationQuaternion
                        );
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
