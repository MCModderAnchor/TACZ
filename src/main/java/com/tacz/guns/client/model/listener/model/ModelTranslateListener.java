package com.tacz.guns.client.model.listener.model;

import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.resource.pojo.model.BonesItem;

import javax.annotation.Nullable;

public class ModelTranslateListener implements AnimationListener {
    private final ModelRendererWrapper rendererWrapper;
    private final @Nullable BonesItem bonesItem;

    public ModelTranslateListener(BedrockAnimatedModel model, ModelRendererWrapper rendererWrapper, String nodeName) {
        this.rendererWrapper = rendererWrapper;
        // 如果当前 node 是根 node（也就是包含于 shouldRender 中），则获取其 bonesItem，以便后续计算相对位移 offset。
        if (model.getShouldRender().contains(rendererWrapper.getModelRenderer())) {
            this.bonesItem = model.getIndexBones().get(nodeName);
        } else {
            this.bonesItem = null;
        }
    }

    @Override
    public void update(float[] values, boolean blend) {
        if (bonesItem != null) {
            // 因为模型是上下颠倒的，因此此处x轴和y轴的偏移也进行取反
            // 因为要达成所有位移都是相对位移，所以如果当前node是根node，则减去根node的pivot坐标。
            if (blend) {
                // 约束组动画是特殊值，不参与混合
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
            if (blend) {
                // 约束组动画是特殊值，不参与混合
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
    public float[] recover() {
        // 目标是让 offset 过渡为 0
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
}
