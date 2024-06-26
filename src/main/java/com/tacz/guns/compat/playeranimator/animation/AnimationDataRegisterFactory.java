package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;

public class AnimationDataRegisterFactory {
    public static void registerData() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.ANIMATION_DATA_ID, 93, player -> new ModifierLayer<>());
    }
}
