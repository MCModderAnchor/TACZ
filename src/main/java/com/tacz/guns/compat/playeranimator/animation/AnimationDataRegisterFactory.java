package com.tacz.guns.compat.playeranimator.animation;

import com.tacz.guns.client.animation.third.AnimationRenderContext;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;

public class AnimationDataRegisterFactory {
    public static void registerData() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.LOWER_ANIMATION, 93, AnimationDataRegisterFactory::createLayer);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.LOOP_UPPER_ANIMATION, 94, AnimationDataRegisterFactory::createLayer);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, 95, AnimationDataRegisterFactory::createLayer);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimatorCompat.ROTATION_ANIMATION, 96, AnimationDataRegisterFactory::createLayer);
    }

    private static PlayerAnimationLayer createLayer(AbstractClientPlayer player) {
        return new PlayerAnimationLayer(() -> AnimationRenderContext.isFirstPersonHand(player));
    }
}
