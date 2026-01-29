package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.model.Perspective;
import com.github.exopandora.shouldersurfing.client.InputHandler;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;

import java.util.Optional;

public class ShoulderSurfingCompatInner {
    public static boolean showCrosshair() {
        Perspective current = Perspective.current();
        return current == Perspective.SHOULDER_SURFING && !InputHandler.FREE_LOOK.isDown() && isAiming();
    }

    // 参考: https://github.com/ChloePrime/Third-Person-Shooting-Zero/blob/master/src/main/java/mod/chloeprime/thirdpersonshooting/client/LocalGunner.java
    private static Optional<IClientPlayerGunOperator> get() {
        return Optional.ofNullable(Minecraft.getInstance().player)
                .map(IClientPlayerGunOperator::fromLocalPlayer);
    }

    public static boolean isAiming() {
        return get().filter(IClientPlayerGunOperator::isAim).isPresent();
    }
}
