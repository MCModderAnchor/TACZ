package com.tac.guns.client.animation.internal;

import com.google.common.collect.Sets;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.AnimationPlan;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.ObjectAnimationRunner;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class GunAnimationStateMachine {
    protected static final Set<Integer> blendingTracks = Sets.newHashSet();
    protected static int trackIndexTop = 0;
    public static final int[] SHOOTING_TRACKS = {blendingTrack(), blendingTrack(), blendingTrack(), blendingTrack(), blendingTrack(), blendingTrack(), blendingTrack(), blendingTrack()};
    public static final int MOVEMENT_TRACK = blendingTrack();
    public static final int MAIN_TRACK = staticTrack();
    public static final int BOLT_CATCH_STATIC_TRACK = staticTrack();
    public static final int HOLDING_POSE_STATIC_TRACK = staticTrack();
    public static final String STATIC_BOLT_CAUGHT_ANIMATION = "static_bolt_caught";
    public static final String STATIC_IDLE_ANIMATION = "static_idle";
    public static final String SHOOT_ANIMATION = "shoot";
    public static final String RELOAD_EMPTY_ANIMATION = "reload_empty";
    public static final String RELOAD_TACTICAL_ANIMATION = "reload_tactical";
    public static final String DRAW_ANIMATION = "draw";
    public static final String PUT_AWAY_ANIMATION = "put_away";
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
    public static final String WALK_AIMING_ANIMATION = "walk_aiming";
    protected AnimationController controller;
    protected boolean noAmmo = false;
    protected boolean onGround = true;
    protected boolean pauseWalkAndRun = false;
    protected boolean isAiming = false;
    //记录开始冲刺时玩家的walk distance，以便让冲刺动画有统一的开头
    protected float baseDistanceWalked = 0.0f;
    protected float keepDistanceWalked = 0.0f;
    protected WalkDirection lastWalkDirection = WalkDirection.NONE;
    protected boolean isWalkAiming = false;

    public GunAnimationStateMachine(AnimationController controller) {
        this.controller = controller;
        for (int i = 0; i < trackIndexTop; i++) {
            controller.setBlending(i, blendingTracks.contains(i));
        }
    }

    protected static int staticTrack() {
        return trackIndexTop++;
    }

    protected static int blendingTrack() {
        int track = trackIndexTop++;
        blendingTracks.add(track);
        return track;
    }

    public void onGunShoot() {
        for(int track : SHOOTING_TRACKS) {
            if (tryRunShootAnimation(track)) {
                return;
            }
        }
        controller.runAnimation(SHOOTING_TRACKS[0], SHOOT_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0f);
    }

    public void onGunReload() {
        if (noAmmo) {
            controller.runAnimation(MAIN_TRACK, RELOAD_EMPTY_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0.2f);
        } else {
            controller.runAnimation(MAIN_TRACK, RELOAD_TACTICAL_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0.2f);
        }
    }

    public void onGunDraw() {
        controller.runAnimation(MAIN_TRACK, DRAW_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0);
    }

    public void onShooterRun(float walkDist) {
        if(isPlayingRunIntroOrLoop()) {
            if (!onGround && !isPlayingRunHold()) {
                controller.runAnimation(MOVEMENT_TRACK, RUN_HOLD_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.6f);
                isWalkAiming = false;
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
            isWalkAiming = false;
            lastWalkDirection = WalkDirection.NONE;
            baseDistanceWalked = walkDist;
        }
    }

    public void onGunPutAway(float putAwayTimeS) {
        controller.runAnimation(MAIN_TRACK, PUT_AWAY_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, putAwayTimeS * 0.75f);
        // 改变 put away 动画的进度，如果刚刚切枪不久，则收枪应当更快。
        ObjectAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner != null) {
            if (runner.isRunning() && PUT_AWAY_ANIMATION.equals(runner.getAnimation().name)) {
                long progress = (long) (Math.max(runner.getAnimation().getMaxEndTimeS() - putAwayTimeS, 0) * 1e9);
                runner.setProgressNs(progress);
                return;
            }
            if (runner.getTransitionTo() != null && PUT_AWAY_ANIMATION.equals(runner.getTransitionTo().getAnimation().name)) {
                long progress = (long) (Math.max(runner.getTransitionTo().getAnimation().getMaxEndTimeS() - putAwayTimeS, 0) * 1e9);
                runner.getTransitionTo().setProgressNs(progress);
            }
        }
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
        isWalkAiming = false;
        lastWalkDirection = WalkDirection.NONE;
        ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
        if (isPlayingRunIntroOrLoop()) {
            deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.3f));
        }
        deque.add(new AnimationPlan(IDLE_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.4f));
        controller.queueAnimation(MOVEMENT_TRACK, deque);
    }

    public void onGunInspect() {
        if (noAmmo) {
            controller.runAnimation(MAIN_TRACK, INSPECT_EMPTY_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0.2f);
        } else {
            controller.runAnimation(MAIN_TRACK, INSPECT_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_STOP, 0.2f);
        }
    }

    public void onShooterWalk(Input input, float walkDist) {
        if (!onGround && !isPlayingIdleAnimation()) {
            controller.runAnimation(MOVEMENT_TRACK, IDLE_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.6f);
            isWalkAiming = false;
            lastWalkDirection = WalkDirection.NONE;
            return;
        }
        if (onGround) {
            // 如果一边走路一边瞄准，则需要播放特定的动画 WALK_AIMING_ANIMATION。
            if (isAiming) {
                if (isWalkAiming) {
                    return;
                }
                isWalkAiming = true;
                lastWalkDirection = WalkDirection.NONE;
                ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
                if (isPlayingRunIntroOrLoop() || isPlayingRunHold()) {
                    deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.3f));
                }
                deque.add(new AnimationPlan(WALK_AIMING_ANIMATION, ObjectAnimation.PlayType.LOOP, 0.3f));
                controller.queueAnimation(MOVEMENT_TRACK, deque);
                baseDistanceWalked = walkDist;
                return;
            }
            WalkDirection direction = WalkDirection.fromInput(input);
            // 同一个方向的动画播放只需要触发一次。
            if (direction == lastWalkDirection) {
                return;
            }
            isWalkAiming = false;
            lastWalkDirection = direction;
            ArrayDeque<AnimationPlan> deque = new ArrayDeque<>();
            if (isPlayingRunIntroOrLoop() || isPlayingRunHold()) {
                deque.add(new AnimationPlan(RUN_END_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.3f));
            }
            switch (direction) {
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
    }

    public void onGunFireSelect() {
    }

    public void onGunCatchBolt() {
        if (!isPlayingAnimation(BOLT_CATCH_STATIC_TRACK, STATIC_BOLT_CAUGHT_ANIMATION)) {
            controller.runAnimation(BOLT_CATCH_STATIC_TRACK, STATIC_BOLT_CAUGHT_ANIMATION, ObjectAnimation.PlayType.LOOP, 0);
        }
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

    public GunAnimationStateMachine setAiming(boolean isAiming) {
        this.isAiming = isAiming;
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

    public boolean isPlayingRunAnimation() {
        return isPlayingAnimation(MOVEMENT_TRACK, RUN_START_ANIMATION, RUN_LOOP_ANIMATION, RUN_HOLD_ANIMATION, RUN_END_ANIMATION);
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
        return isPlayingAnimation(MOVEMENT_TRACK, WALK_FORWARD_ANIMATION, WALK_BACKWARD_ANIMATION, WALK_SIDEWAY_ANIMATION, WALK_AIMING_ANIMATION);
    }

    public boolean isPlayingIdleAnimation() {
        return isPlayingAnimation(MOVEMENT_TRACK, IDLE_ANIMATION);
    }

    public boolean isPlayingShootAnimation() {
        for (int track : SHOOTING_TRACKS) {
            ObjectAnimationRunner runner = controller.getAnimation(track);
            if (runner != null) {
                if (runner.isRunning()) {
                    return true;
                }
                if (runner.isTransitioning() && runner.getTransitionTo() != null) {
                    return true;
                }
            }
        }
        return false;
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

    public void onGunReleaseBolt() {
        controller.removeAnimation(BOLT_CATCH_STATIC_TRACK);
    }

    public void onIdleHoldingPose() {
        if (!isPlayingAnimation(HOLDING_POSE_STATIC_TRACK, STATIC_IDLE_ANIMATION)) {
            controller.runAnimation(HOLDING_POSE_STATIC_TRACK, STATIC_IDLE_ANIMATION, ObjectAnimation.PlayType.LOOP, 0);
        }
    }

    private boolean isNamedWalkAnimation(String animationName) {
        return WALK_SIDEWAY_ANIMATION.equals(animationName) || WALK_FORWARD_ANIMATION.equals(animationName) || WALK_BACKWARD_ANIMATION.equals(animationName)
                || WALK_AIMING_ANIMATION.equals(animationName);
    }

    private boolean tryRunShootAnimation(int track) {
        ObjectAnimationRunner runner = controller.getAnimation(track);
        if (runner != null && runner.isRunning() && SHOOT_ANIMATION.equals(runner.getAnimation().name)) {
            return false;
        }
        if (runner != null && runner.getTransitionTo() != null && SHOOT_ANIMATION.equals(runner.getTransitionTo().getAnimation().name)) {
            return false;
        }
        controller.runAnimation(track, SHOOT_ANIMATION, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0);
        return true;
    }

    protected enum WalkDirection {
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
