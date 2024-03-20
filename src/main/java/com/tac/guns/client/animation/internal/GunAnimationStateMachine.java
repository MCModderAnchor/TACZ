package com.tac.guns.client.animation.internal;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.AnimationPlan;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.ObjectAnimationRunner;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;

public class GunAnimationStateMachine {
    public static final int MAIN_TRACK = 0;
    public static final int MOVEMENT_TRACK = 1;
    public static final String SHOOT_ANIMATION = "shoot";
    public static final String RELOAD_EMPTY_ANIMATION = "reload_empty";
    public static final String RELOAD_TACTICAL_ANIMATION = "reload_tactical";
    public static final String DRAW_ANIMATION = "draw";
    public static final String INSPECT_ANIMATION = "inspect";
    public static final String INSPECT_EMPTY_ANIMATION = "inspect_empty";
    public static final String IDLE_ANIMATION = "idle";
    public static final String RUN_START_ANIMATION = "run_start";
    public static final String RUN_LOOP_ANIMATION = "run";
    public static final String RUN_HOLD_ANIMATION = "run_hold";
    public static final String RUN_END_ANIMATION = "run_end";
    public static final String WALK_FORWARD_ANIMATION = "walk_forward";
    public static final String WALK_SIDEWAY_ANIMATION = "walk_sideway";
    public static final String WALK_BACKWARD_ANIMATION = "walk_backward";
    protected AnimationController controller;
    protected boolean noAmmo;
    protected boolean onGround;
    protected boolean pauseWalkAndRun;

    //记录开始冲刺时玩家的walk distance，以便让冲刺动画有统一的开头
    private float baseDistanceWalked = 0.0f;
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

    public void onShooterRun(float walkDist) {
        if(isPlayingRunIntroOrLoop()) {
            if (!onGround && !isPlayingRunHold()) {
                controller.runAnimation(MOVEMENT_TRACK, RUN_HOLD_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f);
                lastWalkDirection = WalkDirection.NONE;
            }
            return;
        }
        if (onGround) {
            ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
            if (!isPlayingRunHold()) {
                deque.add(new AnimationPlan(RUN_START_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f));
            }
            deque.add(new AnimationPlan(RUN_LOOP_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.2f));
            controller.queueAnimation(MOVEMENT_TRACK, deque);
            lastWalkDirection = WalkDirection.NONE;
            baseDistanceWalked = walkDist;
        }
    }

    public void onShooterWalk(Input input, float walkDist) {
        WalkDirection direction = WalkDirection.fromInput(input);
        if (direction == lastWalkDirection) {
            return;
        }
        lastWalkDirection = direction;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        if (isPlayingRunIntroOrLoop() || isPlayingRunHold()) {
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
        controller.queueAnimation(MOVEMENT_TRACK, deque);
        baseDistanceWalked = walkDist;
    }

    public void onShooterIdle() {
        if (isPlayingIdleAnimation()) {
            return;
        }
        ObjectAnimationRunner runner = controller.getAnimation(MOVEMENT_TRACK);
        if (runner != null && (runner.isRunning() || runner.isTransitioning())) {
            if (!isPlayingWalkAnimation() && !isPlayingRunIntroOrLoop() && !isPlayingRunHold()) {
                return;
            }
        }
        lastWalkDirection = WalkDirection.NONE;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        if (isPlayingRunIntroOrLoop()) {
            deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.4f));
        }
        deque.add(new AnimationPlan(IDLE_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
        controller.queueAnimation(MOVEMENT_TRACK, deque);
    }

    public GunAnimationStateMachine setNoAmmo(boolean noAmmo) {
        this.noAmmo = noAmmo;
        return this;
    }

    public GunAnimationStateMachine setOnGround(boolean onGround) {
        this.onGround = onGround;
        return this;
    }

    public GunAnimationStateMachine setPauseWalkAndRun(boolean pause) {
        this.pauseWalkAndRun = pause;
        return this;
    }

    public AnimationController getController() {
        return controller;
    }

    /**
     * @return 返回当前正在播放的动画是否需要隐藏准心。
     */
    public boolean shouldHideCrossHair() {
        if (isPlayingInspectAnimation()) {
            return true;
        }

        return isPlayingRunHold() || isPlayingRunLoop();
    }

    public void update(float partialTicks, Entity entity) {
        ObjectAnimationRunner runner = controller.getAnimation(MOVEMENT_TRACK);
        if (runner != null) {
            //为了让冲刺和行走动画和原版的viewBobbing相适应，需要手动更新冲刺动画的进度
            //当前动画是run或者正在过渡向run动画的时候，就手动设置run动画的进度。
            float deltaDistanceWalked = entity.walkDist - entity.walkDistO;
            float distanceWalked;
            if (pauseWalkAndRun) {
                // 保持 distanceWalked 与 keepDistanceWalked 相同，即不随时间增长
                distanceWalked = keepDistanceWalked;
                baseDistanceWalked = entity.walkDist + deltaDistanceWalked * partialTicks - keepDistanceWalked;
            } else {
                // distanceWalked 与 keepDistanceWalked 一同随时间增长
                distanceWalked = entity.walkDist + deltaDistanceWalked * partialTicks - baseDistanceWalked;
                keepDistanceWalked = distanceWalked;
            }
            String animationName = runner.getAnimation().name;
            if ((isNamedWalkAnimation(animationName) || RUN_LOOP_ANIMATION.equals(animationName)) && runner.isRunning()) {
                runner.setProgressNs((long) (runner.getAnimation().getMaxEndTimeS() * (distanceWalked % 2f) / 2f * 1e9f));
            }
            if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                animationName = runner.getTransitionTo().getAnimation().name;
                if (isNamedWalkAnimation(animationName) || RUN_LOOP_ANIMATION.equals(animationName)) {
                    runner.getTransitionTo().setProgressNs((long) (runner.getTransitionTo().getAnimation().getMaxEndTimeS() * (distanceWalked % 2f) / 2f * 1e9f));
                }
            }
        }
        controller.update();
    }

    public boolean isPlayingAnimation(int track, @Nonnull String... names) {
        ObjectAnimationRunner runner = controller.getAnimation(track);
        if (runner != null) {
            String animationName = runner.getAnimation().name;
            if (runner.isRunning()) {
                for (String name : names) {
                    if (name.equals(animationName)) {
                        return true;
                    }
                }
            }
            if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                animationName = runner.getTransitionTo().getAnimation().name;
                for (String name : names) {
                    if (name.equals(animationName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isPlayingRunIntroOrLoop() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_LOOP_ANIMATION, RUN_START_ANIMATION);
    }

    public boolean isPlayingRunStart() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_START_ANIMATION);
    }

    public boolean isPlayingRunLoop() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_LOOP_ANIMATION);
    }

    public boolean isPlayingRunHold() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_HOLD_ANIMATION);
    }

    public boolean isPlayingRunEnd() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_END_ANIMATION);
    }

    public boolean isPlayingWalkAnimation() {
        return isPlayingAnimation(MOVEMENT_TRACK, WALK_FORWARD_ANIMATION, WALK_BACKWARD_ANIMATION, WALK_SIDEWAY_ANIMATION);
    }

    public boolean isPlayingIdleAnimation() {
        return isPlayingAnimation(MOVEMENT_TRACK, IDLE_ANIMATION);
    }

    public boolean isPlayingShootAnimation() {
        return isPlayingAnimation(MAIN_TRACK, SHOOT_ANIMATION);
    }

    public boolean isPlayingInspectAnimation() {
        return isPlayingAnimation(MAIN_TRACK, INSPECT_ANIMATION, INSPECT_EMPTY_ANIMATION);
    }

    public boolean isPlayingReloadAnimation() {
        return isPlayingAnimation(MAIN_TRACK, RELOAD_EMPTY_ANIMATION, RELOAD_TACTICAL_ANIMATION);
    }

    public boolean isPlayingDrawAnimation() {
        return isPlayingAnimation(MAIN_TRACK, DRAW_ANIMATION);
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
