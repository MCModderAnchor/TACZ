package com.tac.guns.client.animation.internal;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.ObjectAnimationRunner;

import java.util.Objects;

public class GunAnimationStateMachine {
    public static final int MAIN_TRACK = 0;
    public static final String SHOOT_ANIMATION = "shoot";
    public static final String RELOAD_EMPTY_ANIMATION = "reload_empty";
    public static final String RELOAD_TACTICAL_ANIMATION = "reload_tactical";
    public static final String DRAW_ANIMATION = "draw";
    public static final String INSPECT_ANIMATION = "inspect";
    public static final String INSPECT_EMPTY_ANIMATION = "inspect_empty";
    protected AnimationController controller;
    protected boolean noAmmo;

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
        return INSPECT_ANIMATION.equals(animation.name);
    }

    public void update() {
        controller.update();
    }
}
