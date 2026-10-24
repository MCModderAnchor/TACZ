package com.tacz.guns.compat.playeranimator.animation;

import dev.kosmx.playerAnim.core.data.AnimationFormat;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;

/** Preserve the interpolation used to load TaCZ's Bedrock player animations on PA 1.x. */
public final class PlayerAnimatorEasing {
    private PlayerAnimatorEasing() {
    }

    public static KeyframeAnimation restoreLegacyEasing(KeyframeAnimation animation) {
        if (animation.animationFormat != AnimationFormat.JSON_MC_ANIM
                || animation.getBodyParts().values().stream()
                .flatMap(part -> java.util.Arrays.stream(channels(part)))
                .filter(java.util.Objects::nonNull)
                .flatMap(state -> state.getKeyFrames().stream())
                .noneMatch(frame -> frame.ease == Ease.CATMULLROM)) {
            return animation;
        }

        // PA 1.x mapped Bedrock catmullrom to INOUTSINE. PA 2.0.1's CATMULLROM
        // evaluates to 1 + t, extrapolating past the next keyframe and snapping back.
        var builder = animation.mutableCopy();
        for (var part : builder.getBodyParts().values()) {
            for (var state : channels(part)) {
                if (state == null) {
                    continue;
                }
                for (int index = 0; index < state.length(); index++) {
                    var frame = state.getKeyFrames().get(index);
                    if (frame.ease == Ease.CATMULLROM) {
                        state.replace(new KeyframeAnimation.KeyFrame(frame.tick, frame.value, Ease.INOUTSINE, frame.easingArg), index);
                    }
                }
            }
        }
        return builder.build();
    }

    private static KeyframeAnimation.StateCollection.State[] channels(KeyframeAnimation.StateCollection part) {
        return new KeyframeAnimation.StateCollection.State[]{
                part.x, part.y, part.z, part.pitch, part.yaw, part.roll,
                part.bend, part.bendDirection, part.scaleX, part.scaleY, part.scaleZ
        };
    }
}
