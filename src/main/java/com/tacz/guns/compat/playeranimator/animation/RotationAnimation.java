package com.tacz.guns.compat.playeranimator.animation;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationContainer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

/** Keep the correction inside the outer layer's fade-out, together with the existing empty animation. */
public class RotationAnimation extends AnimationContainer<AdjustmentModifier> {
    private final KeyframeAnimation data;

    public RotationAnimation(KeyframeAnimation data, AdjustmentModifier adjustment) {
        super(adjustment);
        this.data = data;
        adjustment.setAnim(new KeyframeAnimationPlayer(data).setFirstPersonMode(FirstPersonMode.DISABLED));
    }

    public KeyframeAnimation getData() {
        return data;
    }
}
