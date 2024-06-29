package com.tacz.guns.api.client.animation;

import com.tacz.guns.client.animation.AnimationController;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 无限动画状态机的实现。
 * @param <T> 状态机上下文类型
 */
public class AnimationStateMachine<T extends StateContext> {
    /**
     * 状态机当前的状态列表
     */
    private List<AnimationState<T>> currentStates;

    /**
     * 状态机上下文，承载动画状态更新可能需要的各种参数
     */
    private T context;

    /**
     * 状态机上下文的实例 Supplier
     */
    private final @Nonnull NonNullSupplier<T> contextSupplier;

    /**
     * 初始状态 Supplier
     */
    private final @Nullable Supplier<Iterable<? extends AnimationState<T>>> statesSupplier;

    /**
     * 状态机控制的动画控制器
     */
    private final @Nonnull AnimationController animationController;

    /**
     * 在状态机初始化完成后调用
     */
    private @Nullable Consumer<T> initializeFunc;

    /**
     * 在状态机退出之前调用
     */
    private @Nullable Consumer<T> exitFunc;

    /**
     * @param animationController 动画状态机控制的动画控制器
     * @param contextSupplier 状态机初始化时调用，获取一个上下文实例作为动画状态机的唯一上下文实例。
     * @param statesSupplier 状态机初始化时调用，将提供的状态加入状态机的当前状态列表，作为初始状态。
     *                       注意，这些状态的 entryAction 会被调用。
     * @see #initialize()
     */
    public AnimationStateMachine(@Nonnull AnimationController animationController,
                                 @Nonnull NonNullSupplier<T> contextSupplier,
                                 @Nullable Supplier<Iterable<? extends AnimationState<T>>> statesSupplier) {
        this.animationController = Objects.requireNonNull(animationController);
        this.contextSupplier = Objects.requireNonNull(contextSupplier);
        this.statesSupplier = statesSupplier;
    }

    /**
     * 每一次每一次渲染模型之前调用。
     * 会同时更新状态列表中的所有状态，并更新动画控制器。
     *
     * @throws IllegalStateException 若状态机在初始化之前调用此方法，抛出异常。
     * @see AnimationState#update(StateContext)
     * @see AnimationController#update()
     */
    public void update() {
        if (currentStates == null) {
            throw new IllegalStateException("State machine has not been initialized");
        }
        currentStates.forEach(state -> state.update(context));
        animationController.update();
    }

    /**
     * 对状态机进行一次输入，可能触发状态转移。
     *
     * @param condition 输入
     * @throws IllegalStateException 若状态机在初始化之前调用此方法，抛出异常。
     */
    public void trigger(String condition) {
        if (currentStates == null) {
            throw new IllegalStateException("State machine has not been initialized");
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
     * 调用此方法对状态机进行初始化。
     */
    public void initialize() {
        this.currentStates = new LinkedList<>();
        // 初始化状态机上下文
        this.context = Objects.requireNonNull(contextSupplier.get());
        this.context.stateMachine = this;
        // 将提供的初始状态加入状态列表，并调用它们的 entryAction 方法。
        Optional.ofNullable(statesSupplier)
                .map(Supplier::get)
                .ifPresent(list -> list.forEach(state -> {
                    currentStates.add(state);
                    state.entryAction(context);
                }));
        if (this.initializeFunc != null) {
            initializeFunc.accept(context);
        }
    }

    /**
     * 调用此方法使状态机退出。
     */
    public void exit() {
        if (this.exitFunc != null) {
            exitFunc.accept(context);
        }
        currentStates.forEach(state -> state.exitAction(context));
        this.currentStates = null;
        this.context = null;
    }

    /**
     * @return 状态机控制的动画控制器
     */
    public @Nonnull AnimationController getAnimationController() {
        return animationController;
    }

    /**
     * @return 当前的状态上下文
     * @throws IllegalStateException 若状态机在初始化之前调用此方法，抛出异常。
     */
    public @Nonnull T getContext() {
        if (context == null) {
            throw new IllegalStateException("State machine has not been initialized");
        }
        return context;
    }

    /**
     * 设置状态机自定义的初始化动作。
     * @param initializeFunc 在状态机初始化后调用。
     */
    public void setInitializeFunc(@Nullable Consumer<T> initializeFunc) {
        this.initializeFunc = initializeFunc;
    }

    /**
     * 设置状态机自定义的退出动作。
     * @param exitFunc 在状态机退出前调用。
     */
    public void setExitFunc(@Nullable Consumer<T> exitFunc) {
        this.exitFunc = exitFunc;
    }
}
