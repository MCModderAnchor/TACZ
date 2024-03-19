package com.tac.guns.client.animation.internal;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.AnimationPlan;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.ObjectAnimationRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Objects;

public class GunAnimationStateMachine {
    public static final int MAIN_TRACK = 0;
    public static final String SHOOT_ANIMATION = "shoot";
    public static final String RELOAD_EMPTY_ANIMATION = "reload_empty";
    public static final String RELOAD_TACTICAL_ANIMATION = "reload_tactical";
    public static final String DRAW_ANIMATION = "draw";
    public static final String INSPECT_ANIMATION = "inspect";
    public static final String INSPECT_EMPTY_ANIMATION = "inspect_empty";
    public static final String IDLE_ANIMATION = "idle";
    public static final String RUN_START_ANIMATION = "run_start";
    public static final String RUN_LOOP_ANIMATION = "run";
    public static final String RUN_END_ANIMATION = "run_end";
    public static final String WALK_FORWARD_ANIMATION = "walk_forward";
    public static final String WALK_SIDEWAY_ANIMATION = "walk_sideway";
    public static final String WALK_BACKWARD_ANIMATION = "walk_backward";
    protected AnimationController controller;
    protected boolean noAmmo;
    //记录开始冲刺时玩家的walk distance，以便让冲刺动画有统一的开头
    private float baseDistanceWalked = 0.0f;
    //玩家以冲刺状态脱离地面时，需要保持walk distance不变以保持姿态。
    private float keepDistanceWalked = 0.0f;
    private WalkDirection lastWalkDirection = WalkDirection.NONE;

    public GunAnimationStateMachine(AnimationController controller) {
        this.controller = controller;
    }

    public void onGunShoot() {
        controller.runAnimation(MAIN_TRACK, SHOOT_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.02f);
    }

    public void onGunFireSelect() {
    }

    public void onGunReload() {
        if (noAmmo) {
            controller.runAnimation(MAIN_TRACK, RELOAD_EMPTY_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        } else {
            controller.runAnimation(MAIN_TRACK, RELOAD_TACTICAL_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        }
    }

    public void onGunDraw() {
        controller.runAnimation(MAIN_TRACK, DRAW_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0);
    }

    public void onGunInspect() {
        if (noAmmo) {
            controller.runAnimation(MAIN_TRACK, INSPECT_EMPTY_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        } else {
            controller.runAnimation(MAIN_TRACK, INSPECT_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        }
    }

    public void onShooterRun(float baseDistanceWalked) {
        // 可以从 idle、walk、inspect 转移到这个状态
        if(isPlayingRunIntroOrLoop()) {
            return;
        }
        lastWalkDirection = WalkDirection.NONE;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        deque.add(new AnimationPlan(RUN_START_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f));
        deque.add(new AnimationPlan(RUN_LOOP_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.2f));
        controller.queueAnimation(MAIN_TRACK, deque);
        this.baseDistanceWalked = baseDistanceWalked;
    }

    public void onShooterWalk(Input input, float baseDistanceWalked) {
        // 可以从 idle、run、inspect 转移到这个状态
        WalkDirection direction = WalkDirection.fromInput(input);
        if (direction == lastWalkDirection) {
            return;
        }
        lastWalkDirection = direction;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        if (isPlayingRunIntroOrLoop()) {
            deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.4f));
        }
        switch (direction){
            case FORWARD -> {
                deque.add(new AnimationPlan(WALK_FORWARD_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
            }
            case BACKWARD -> {
                deque.add(new AnimationPlan(WALK_BACKWARD_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
            }
            case SIDE_WAY -> {
                deque.add(new AnimationPlan(WALK_SIDEWAY_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
            }
        }
        controller.queueAnimation(MAIN_TRACK, deque);
        this.baseDistanceWalked = baseDistanceWalked;
    }

    public void onPlayerIdle() {
        if (isPlayingIdleAnimation()) {
            return;
        }
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null && (runner.isRunning() || runner.isTransitioning()) && !isPlayingWalkAnimation() && !isPlayingRunIntroOrLoop()) {
            return;
        }
        lastWalkDirection = WalkDirection.NONE;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        if (isPlayingRunIntroOrLoop()) {
            deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.4f));
        }
        deque.add(new AnimationPlan(IDLE_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
        controller.queueAnimation(MAIN_TRACK, deque);
    }

    public GunAnimationStateMachine setNoAmmo(boolean noAmmo) {
        this.noAmmo = noAmmo;
        return this;
    }

    public AnimationController getController() {
        return controller;
    }

    /**
     * @return 返回当前正在播放的动画是否需要隐藏准心。
     */
    public boolean shouldHideCrossHair() {
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner == null || (!runner.isRunning() && !runner.isTransitioning())) {
            return false;
        }
        ObjectAnimationRunner transitionTo = runner.getTransitionTo();
        ObjectAnimation animation = Objects.requireNonNullElse(transitionTo, runner).getAnimation();
        return INSPECT_ANIMATION.equals(animation.name) || INSPECT_EMPTY_ANIMATION.equals(animation.name) || RUN_LOOP_ANIMATION.equals(animation.name);
    }

    public void update(float partialTicks) {
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null) {
            //为了让冲刺和行走动画和原版的viewBobbing相适应，需要手动更新冲刺动画的进度
            //当前动画是run或者正在过渡向run动画的时候，就手动设置run动画的进度。
            Entity entity = Minecraft.getInstance().getCameraEntity();
            if (entity instanceof Player playerEntity) {
                float deltaDistanceWalked = playerEntity.walkDist - playerEntity.walkDistO;
                float distanceWalked;
                if (playerEntity.isOnGround()) {
                    distanceWalked = playerEntity.walkDist + deltaDistanceWalked * partialTicks - baseDistanceWalked;
                }
                else {
                    distanceWalked = keepDistanceWalked;
                    baseDistanceWalked = playerEntity.walkDist + deltaDistanceWalked * partialTicks - keepDistanceWalked;
                }
                keepDistanceWalked = distanceWalked;
                String animationName = runner.getAnimation().name;
                if ((isNamedWalkAnimation(animationName) || RUN_LOOP_ANIMATION.equals(animationName)) && runner.isRunning()) {
                    runner.setProgressNs((long) (runner.getAnimation().getMaxEndTimeS() * (distanceWalked % 2f) / 2f * 1e9f));
                }
                if (runner.isTransitioning() && runner.getTransitionTo()!= null){
                    animationName = runner.getTransitionTo().getAnimation().name;
                    if (isNamedWalkAnimation(animationName) || RUN_LOOP_ANIMATION.equals(animationName)){
                        runner.getTransitionTo().setProgressNs((long) (runner.getTransitionTo().getAnimation().getMaxEndTimeS() * (distanceWalked % 2f) / 2f * 1e9f));
                    }
                }
            }
        }
        controller.update();
    }

    private boolean isPlayingRunIntroOrLoop() {
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null) {
            String animationName = runner.getAnimation().name;
            if ((RUN_LOOP_ANIMATION.equals(animationName) || RUN_START_ANIMATION.equals(animationName)) && runner.isRunning()) {
                return true;
            }
            if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                animationName = runner.getTransitionTo().getAnimation().name;
                return RUN_LOOP_ANIMATION.equals(animationName) || RUN_START_ANIMATION.equals(animationName);
            }
        }
        return false;
    }

    private boolean isPlayingWalkAnimation() {
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null) {
            String animationName = runner.getAnimation().name;
            if (isNamedWalkAnimation(animationName) && runner.isRunning()) {
                return true;
            }
            if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                animationName = runner.getTransitionTo().getAnimation().name;
                return isNamedWalkAnimation(animationName);
            }
        }
        return false;
    }

    private boolean isPlayingIdleAnimation() {
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null) {
            String animationName = runner.getAnimation().name;
            if (IDLE_ANIMATION.equals(animationName) && runner.isRunning()) {
                return true;
            }
            if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                animationName = runner.getTransitionTo().getAnimation().name;
                return IDLE_ANIMATION.equals(animationName);
            }
        }
        return false;
    }

    private boolean isNamedWalkAnimation(String animationName) {
        return WALK_SIDEWAY_ANIMATION.equals(animationName) || WALK_FORWARD_ANIMATION.equals(animationName) || WALK_BACKWARD_ANIMATION.equals(animationName);
    }

    private enum WalkDirection{
        FORWARD,
        SIDE_WAY,
        BACKWARD,
        NONE;

        public static WalkDirection fromInput(Input input){
            if (input.up) {
                return FORWARD;
            }
            if (input.down) {
                return BACKWARD;
            }
            if (input.left || input.right) {
                return SIDE_WAY;
            }
            return NONE;
        }
    }
}
