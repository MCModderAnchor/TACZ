package com.tacz.guns.api.client.animation;

public interface AnimationState {
    /**
     * @param context 状态上下文，承载状态行为可能需要的各种参数
     *
     * 每一帧渲染模型前都会调用。
     */
    void update(StateContext context);

    /**
     * @param context 状态上下文，承载状态行为可能需要的各种参数
     *
     * 触发状态转移进入此状态时调用。
     * @see AnimationStateMachine#trigger(java.lang.String)
     */
    void entryAction(StateContext context);

    /**
     * @param context 状态上下文，承载状态行为可能需要的各种参数
     *
     * 触发状态转移退出此状态时调用。
     * @see AnimationStateMachine#trigger(java.lang.String)
     */
    void exitAction(StateContext context);

    /**
     * 每当状态机接受输入时，调用此方法。
     *
     * @param context 状态上下文，承载状态行为可能需要的各种参数
     * @param condition 状态机接受的输入
     * @return 返回转移后的状态，或者返回 Null 表示无需状态转移。
     * @see AnimationStateMachine#trigger(java.lang.String)
     */
    AnimationState transition(StateContext context, String condition);
}
