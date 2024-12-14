package com.tacz.guns.client.animation.statemachine;

import com.tacz.guns.api.client.animation.statemachine.AnimationStateContext;

public class ItemAnimationStateContext extends AnimationStateContext {
    private float putAwayTime = 0f;

    /**
     * 获取收起物品动画的建议时长，它只是计算结果，具体如何生效依赖于状态机实现。
     * @return 收起物品动画的建议时长
     */
    public float getPutAwayTime() {
        return putAwayTime;
    }

    /**
     * 状态机脚本不要调用此方法。此方法用于设置物品动画的建议时长
     */
    public void setPutAwayTime(float putAwayTime) {
        this.putAwayTime = putAwayTime;
    }
}
