package com.tacz.guns.api.client.animation;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class AnimationStateMachine {
    /**
     * 状态机当前的状态列表
     */
    private final List<AnimationState> currentStates = new ArrayList<>();

    /**
     * 承载动画状态可能需要的各种参数
     */
    private final StateContext context = new StateContext();

    /**
     * 每一帧渲染模型之前调用（仅第一人称）。
     */
    public void update(float partialTicks, Entity entity) {
        context.setPartialTicks(partialTicks);
        context.setEntity(entity);
        currentStates.forEach(state -> state.update(context));
    }

    /**
     * 对状态机进行一次输入，可能触发状态转移。
     *
     * @param condition 输入
     */
    public void trigger(String condition) {
        ListIterator<AnimationState> iterator = currentStates.listIterator();
        // 迭代状态列表，如果需要状态转移，则将转移后的状态替换进列表
        while (iterator.hasNext()) {
            AnimationState state = iterator.next();
            AnimationState nextState = state.transition(context, condition);
            if (nextState != null) {
                state.exitAction(context);
                iterator.set(nextState);
                nextState.entryAction(context);
            }
        }
    }

    /**
     * 为状态机的当前状态列表添加一个状态。
     *
     * @param state 需要添加的状态
     */
    public void addState(AnimationState state) {
        this.currentStates.add(state);
    }
}
