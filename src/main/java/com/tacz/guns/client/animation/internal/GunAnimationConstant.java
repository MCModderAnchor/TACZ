package com.tacz.guns.client.animation.internal;

public final class GunAnimationConstant {
    /**
     * 空仓挂机
     */
    public static final String STATIC_BOLT_CAUGHT_ANIMATION = "static_bolt_caught";
    /**
     * 默认持枪动作
     */
    public static final String STATIC_IDLE_ANIMATION = "static_idle";
    /**
     * 射击
     */
    public static final String SHOOT_ANIMATION = "shoot";
    /**
     * 刺刀攻击，会按照 1->2->3 的顺序连续播放
     */
    public static final String BAYONET_ANIMATION_1 = "melee_bayonet_1";
    public static final String BAYONET_ANIMATION_2 = "melee_bayonet_2";
    public static final String BAYONET_ANIMATION_3 = "melee_bayonet_3";
    /**
     * 空仓换弹
     */
    public static final String RELOAD_EMPTY_ANIMATION = "reload_empty";
    /**
     * 装了扩容弹匣后的空仓换弹
     */
    public static final String RELOAD_EMPTY_EXTENDED_ANIMATION = "reload_empty_extended";
    /**
     * 拉栓
     */
    public static final String BOLT_ANIMATION = "bolt";
    /**
     * 战术换弹
     */
    public static final String RELOAD_TACTICAL_ANIMATION = "reload_tactical";
    /**
     * 装了扩容弹匣后的战术换弹
     */
    public static final String RELOAD_TACTICAL_EXTENDED_ANIMATION = "reload_tactical_extended";
    /**
     * 切枪动画，切入
     */
    public static final String DRAW_ANIMATION = "draw";
    /**
     * 切枪动画，切出
     */
    public static final String PUT_AWAY_ANIMATION = "put_away";
    /**
     * 检视动画，非空仓
     */
    public static final String INSPECT_ANIMATION = "inspect";
    /**
     * 检视动画，空仓
     */
    public static final String INSPECT_EMPTY_ANIMATION = "inspect_empty";
    /**
     * 静止时的动画
     */
    public static final String IDLE_ANIMATION = "idle";
    /**
     * 跑步动画，起始部分
     */
    public static final String RUN_START_ANIMATION = "run_start";
    /**
     * 跑步动画
     */
    public static final String RUN_LOOP_ANIMATION = "run";
    /**
     * 跑步持枪动画
     */
    public static final String RUN_HOLD_ANIMATION = "run_hold";
    /**
     * 跑步动画，结束部分
     */
    public static final String RUN_END_ANIMATION = "run_end";
    /**
     * 向前行走动画
     */
    public static final String WALK_FORWARD_ANIMATION = "walk_forward";
    /**
     * 侧向行走动画
     */
    public static final String WALK_SIDEWAY_ANIMATION = "walk_sideway";
    /**
     * 后退行走动画
     */
    public static final String WALK_BACKWARD_ANIMATION = "walk_backward";
    /**
     * 行走时瞄准动画
     */
    public static final String WALK_AIMING_ANIMATION = "walk_aiming";
}
