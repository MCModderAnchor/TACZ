package com.tacz.guns.api.client.animation.statemachine;

import com.tacz.guns.api.client.animation.AnimationController;

import java.util.function.Consumer;

public class LuaAnimationStateMachine<T extends AnimationStateContext> extends AnimationStateMachine<T> {
    Consumer<T> initializeFunc;
    Consumer<T> exitFunc;

    /**
     * 此方法不应该被直接调用，而是应该通过工厂生成实例
     *
     * @param animationController 动画状态机控制的动画控制器
     * @see LuaStateMachineFactory
     */
    LuaAnimationStateMachine(AnimationController animationController) {
        super(animationController);
    }

    @Override
    public void initialize() {
        this.initializeFunc.accept(this.context);
        super.initialize();
    }

    @Override
    public void exit() {
        this.exitFunc.accept(this.context);
        super.exit();
    }
}
