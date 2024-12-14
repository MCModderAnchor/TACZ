package com.tacz.guns.api.client.animation.statemachine;

import com.tacz.guns.api.client.animation.AnimationController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 无限动画状态机的实现。
 * @param <T> 状态机上下文类型
 */
public class AnimationStateMachine<T extends AnimationStateContext> {
    /**
     * 状态机当前的状态列表
     */
    private List<AnimationState<T>> currentStates;

    /**
     * 状态机上下文，承载动画状态更新可能需要的各种参数
     */
    protected T context;

    /**
     * 初始状态 Supplier
     */
    private Supplier<Iterable<? extends AnimationState<T>>> statesSupplier;

    /**
     * 状态机控制的动画控制器
     */
    private final @Nonnull AnimationController animationController;

    /**
     * @param animationController 动画状态机控制的动画控制器
     */
    public AnimationStateMachine(@Nonnull AnimationController animationController) {
        this.animationController = Objects.requireNonNull(animationController);
    }

    /**
     * 每一次每一次渲染模型之前调用。
     * 会同时更新状态列表中的所有状态，并更新动画控制器。
     *
     * @see AnimationState#update(AnimationStateContext)
     * @see AnimationController#update()
     */
    public void update() {
        if (context != null && currentStates != null) {
            currentStates.forEach(state -> state.update(context));
        }
        animationController.update();
    }

    /**
     * 非第一人称渲染时调用调用，不会讲动画数据写入模型，但是仍会更新状态
     * 也会播放声音
     *
     * @see AnimationState#update(AnimationStateContext)
     * @see AnimationController#updateSoundOnly()
     */
    public void visualUpdate() {
        if (context != null && currentStates != null) {
            currentStates.forEach(state -> state.update(context));
        }
        animationController.updateSoundOnly();
    }

    /**
     * 对状态机进行一次输入，可能触发状态转移。
     *
     * @param condition 输入
     */
    public void trigger(String condition) {
        if (context == null || currentStates == null) {
            return;
        }
        // 迭代状态列表，如果需要状态转移，则将转移后的状态替换进列表
        ListIterator<AnimationState<T>> iterator = currentStates.listIterator();
        while (iterator.hasNext()) {
            AnimationState<T> state = iterator.next();
            AnimationState<T> nextState = state.transition(context, condition);
            if (nextState != null) {
                state.exitAction(context);
                iterator.set(nextState);
                nextState.entryAction(context);
            }
        }
    }

    /**
     * 调用此方法对状态机进行初始化。会触发状态的 entry action.<p>
     * 调用此方法之前，需要满足以下条件：<p>
     * 1. context 已经被初始化<p>
     * 2. 状态机处于未初始化状态（首次创建或者调用 exit 方法可进入此状态）
     * @see AnimationState#entryAction(AnimationStateContext)
     */
    public void initialize() {
        if (context == null) {
            throw new IllegalStateException("Context must not be null before initialization");
        }
        if (currentStates != null) {
            throw new IllegalStateException("State machine is already initialized");
        }
        this.currentStates = new LinkedList<>();
        // 将提供的初始状态加入状态列表，并调用它们的 entryAction 方法。
        Optional.ofNullable(statesSupplier)
                .map(Supplier::get)
                .ifPresent(list -> list.forEach(state -> {
                    currentStates.add(state);
                    state.entryAction(context);
                }));
    }

    /**
     * 调用此方法使状态机退出，会触发状态的 exit action.
     * @see AnimationState#exitAction(AnimationStateContext)
     */
    public void exit() {
        checkNullPointer();
        // 调用状态列表内所有状态的 exit action。
        currentStates.forEach(state -> state.exitAction(context));
        this.currentStates = null;
    }

    /**
     * @return 状态机控制的动画控制器
     */
    public @Nonnull AnimationController getAnimationController() {
        return animationController;
    }

    public boolean isInitialized() {
        return currentStates != null;
    }

    /**
     * @return 当前的状态上下文
     */
    public @Nullable T getContext() {
        return context;
    }

    public void processContextIfExist(Consumer<T> consumer) {
        if (context != null) {
            consumer.accept(context);
        }
    }

    /**
     * 设置状态机的上下文。在状态机进行其他操作之前，务必调用此方法将 context 初始化。
     * 在状态机 initialize 执行之后，无法执行本方法，必须先调用 exit 方法退出状态机.
     * 这么做是因为需要确保状态机在一个运行周期内使用的 context 是唯一的。
     */
    public void setContext(@Nonnull T context) {
        AnimationStateMachine<?> stateMachine = context.getStateMachine();
        if (stateMachine != null && stateMachine != this) {
            throw new IllegalStateException("Context is already used");
        }
        if (currentStates != null) {
            throw new IllegalStateException("State machine is already initialized, call exit() first");
        }
        if (this.context != null) {
            this.context.setStateMachine(null);
        }
        context.setStateMachine(this);
        this.context = context;
    }

    /**
     * 状态机初始化时调用，将提供的状态加入状态机的当前状态列表，作为初始状态。
     * 注意，这些状态的 entryAction 会被调用。
     * @param statesSupplier 初始状态列表的 Supplier
     */
    public void setStatesSupplier(Supplier<Iterable<? extends AnimationState<T>>> statesSupplier) {
        this.statesSupplier = statesSupplier;
    }

    private void checkNullPointer(){
        if (context == null) {
            throw new IllegalStateException("Context has not been initialized");
        }
        if (currentStates == null) {
            throw new IllegalStateException("State machine has not been initialized");
        }
    }
}
