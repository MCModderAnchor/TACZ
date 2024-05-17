package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;

public class ModelRendererWrapper {
    private final BedrockPart modelRenderer;

    public ModelRendererWrapper(BedrockPart modelRenderer) {
        this.modelRenderer = modelRenderer;
    }

    public BedrockPart getModelRenderer() {
        return modelRenderer;
    }

    public float getRotateAngleX() {
        return modelRenderer.xRot;
    }

    public void setRotateAngleX(float xRot) {
        modelRenderer.xRot = xRot;
    }

    public float getRotateAngleY() {
        return modelRenderer.yRot;
    }

    public void setRotateAngleY(float yRot) {
        modelRenderer.yRot = yRot;
    }

    public float getRotateAngleZ() {
        return modelRenderer.zRot;
    }

    public void setRotateAngleZ(float zRot) {
        modelRenderer.zRot = zRot;
    }

    public float getOffsetX() {
        return modelRenderer.offsetX;
    }

    public void setOffsetX(float offsetX) {
        modelRenderer.offsetX = offsetX;
    }

    public float getOffsetY() {
        return modelRenderer.offsetY;
    }

    public void setOffsetY(float offsetY) {
        modelRenderer.offsetY = offsetY;
    }

    public float getOffsetZ() {
        return modelRenderer.offsetZ;
    }

    public void setOffsetZ(float offsetZ) {
        modelRenderer.offsetZ = offsetZ;
    }

    public void addOffsetX(float offsetX) {
        setOffsetX(getOffsetX() + offsetX);
    }

    public void addOffsetY(float offsetY) {
        setOffsetY(getOffsetY() + offsetY);
    }

    public void addOffsetZ(float offsetZ) {
        setOffsetZ(getOffsetZ() + offsetZ);
    }

    public float getRotationPointX() {
        return modelRenderer.x;
    }

    public float getRotationPointY() {
        return modelRenderer.y;
    }

    public float getRotationPointZ() {
        return modelRenderer.z;
    }

    public boolean isHidden() {
        return !modelRenderer.visible;
    }

    public void setHidden(boolean hidden) {
        modelRenderer.visible = !hidden;
    }

    public float getInitRotateAngleX() {
        return modelRenderer.getInitRotX();
    }

    public float getInitRotateAngleY() {
        return modelRenderer.getInitRotY();
    }

    public float getInitRotateAngleZ() {
        return modelRenderer.getInitRotZ();
    }

    public Quaternionf getAdditionalQuaternion() {
        return modelRenderer.additionalQuaternion;
    }

    public void setAdditionalQuaternion(Quaternionf quaternion) {
        modelRenderer.additionalQuaternion = quaternion;
    }

    public float getScaleX() {
        return modelRenderer.xScale;
    }

    public void setScaleX(float scaleX) {
        modelRenderer.xScale = scaleX;
    }

    public float getScaleY() {
        return modelRenderer.yScale;
    }

    public void setScaleY(float scaleY) {
        modelRenderer.yScale = scaleY;
    }

    public float getScaleZ() {
        return modelRenderer.zScale;
    }

    public void setScaleZ(float scaleZ) {
        modelRenderer.zScale = scaleZ;
    }

    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay) {
        modelRenderer.render(poseStack, transformType, consumer, light, overlay);
    }
}
