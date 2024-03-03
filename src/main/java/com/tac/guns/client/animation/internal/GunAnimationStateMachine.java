package com.tac.guns.client.animation.internal;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.ObjectAnimation;

public class GunAnimationStateMachine {
    public static final int MAIN_TRACK = 0;
    protected AnimationController controller;
    protected boolean noAmmo;

    public GunAnimationStateMachine(AnimationController controller) {
        this.controller = controller;
    }

    public void onGunShoot() {
        controller.runAnimation(MAIN_TRACK, "shoot", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.02f);
    }

    public void onGunFireSelect() {
    }

    public void onGunReload() {
        if (noAmmo) {
            controller.runAnimation(MAIN_TRACK, "reload_empty", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        } else {
            controller.runAnimation(MAIN_TRACK, "reload_norm", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
        }
    }

    public void onGunDraw() {
        controller.runAnimation(MAIN_TRACK, "draw", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0);
    }

    public void onGunInspect() {
        controller.runAnimation(MAIN_TRACK, "inspect", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.2f);
    }

    public GunAnimationStateMachine setNoAmmo(boolean noAmmo) {
        this.noAmmo = noAmmo;
        return this;
    }

    public AnimationController getController() {
        return controller;
    }

    public void update() {
        controller.update();
    }
}
