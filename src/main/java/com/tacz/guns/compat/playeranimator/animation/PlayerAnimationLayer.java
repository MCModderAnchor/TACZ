package com.tacz.guns.compat.playeranimator.animation;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class PlayerAnimationLayer extends ModifierLayer<IAnimation> {
    // Retain normal overlapping fades, but bound recursion even when entities stop ticking.
    private static final int MAX_FADES = 8;
    private final BooleanSupplier suppressTransforms;

    public PlayerAnimationLayer(BooleanSupplier suppressTransforms) {
        this.suppressTransforms = suppressTransforms;
    }

    @Override
    public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fade, @Nullable IAnimation animation, boolean fadeFromNothing) {
        if (getAnimation() == animation) {
            return;
        }
        while (size() >= MAX_FADES) {
            removeModifier(0);
        }
        super.replaceAnimationWithFade(fade, animation, fadeFromNothing);
    }

    public void setAnimationImmediately(@Nullable IAnimation animation) {
        while (size() > 0) {
            removeModifier(0);
        }
        setAnimation(animation);
    }

    public void clearAnimation() {
        setAnimationImmediately(null);
    }

    @Override
    public @NotNull Vec3f get3DTransform(@NotNull String modelName, @NotNull TransformType type, float tickDelta, @NotNull Vec3f value0) {
        return suppressTransforms.getAsBoolean() ? value0 : super.get3DTransform(modelName, type, tickDelta, value0);
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        return FirstPersonMode.DISABLED;
    }
}
