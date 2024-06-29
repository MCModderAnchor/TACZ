package com.tacz.guns.api.client.animation;

import com.tacz.guns.client.animation.AnimationController;

public class StateContext {
    AnimationStateMachine<?> stateMachine;

    public AnimationStateMachine<?> getStateMachine() {
        return stateMachine;
    }

    public AnimationController getAnimationController() {
        return stateMachine.getAnimationController();
    }
}
