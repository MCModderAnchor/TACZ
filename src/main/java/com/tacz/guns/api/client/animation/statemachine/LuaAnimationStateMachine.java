package com.tacz.guns.api.client.animation.statemachine;

import com.tacz.guns.api.client.animation.AnimationController;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class LuaAnimationStateMachine<T extends AnimationStateContext> extends AnimationStateMachine<LuaContextWrapper<T>> {
    Consumer<LuaContextWrapper<T>> initializeFunc;
    Consumer<LuaContextWrapper<T>> exitFunc;

    /**
     * 此方法不应该被直接调用，而是应该通过工厂生成实例
     *
     * @param animationController 动画状态机控制的动画控制器
     * @see LuaStateMachineFactory
     */
    LuaAnimationStateMachine(AnimationController animationController) {
        super(animationController);
    }

    /**
     * 优化后的更新 context 方法。
     * @param context 上下文实例
     */
    public void setContextOverride(@Nonnull T context) {
        if (this.context == null) {
            this.context = new LuaContextWrapper<>(context);
        } else {
            T oldContext = this.context.getContext();
            if (oldContext != context) {
                if (areSameClass(oldContext, context)) {
                    this.context.setContext(context);
                } else {
                    this.context = new LuaContextWrapper<>(context);
                }
            }
        }
    }

    /**
     * 获取状态机当前上下文
     * @return 上下文的实例
     */
    public T getContextOverride() {
        if (this.context == null) {
            return null;
        }
        return this.context.getContext();
    }

    @Override
    public void initialize() {
        super.initialize();
        this.initializeFunc.accept(this.context);
    }

    @Override
    public void exit() {
        this.exitFunc.accept(this.context);
        super.exit();
    }

    /**
     * @deprecated
     * @see LuaAnimationStateMachine#getContextOverride()
     */
    @Deprecated
    @Override
    public LuaContextWrapper<T> getContext(){
        throw new UnsupportedOperationException("call getContextOverride instead");
    }

    /**
     * @deprecated
     * @see LuaAnimationStateMachine#setContextOverride(AnimationStateContext)
     */
    @Deprecated
    @Override
    public void setContext(@Nonnull LuaContextWrapper<T> ignore){
        throw new UnsupportedOperationException("call setContextOverride instead");
    }

    private static boolean areSameClass(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.getClass().equals(obj2.getClass());
    }
}
