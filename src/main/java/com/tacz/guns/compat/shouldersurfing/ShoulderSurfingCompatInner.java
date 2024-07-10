package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.model.Perspective;
import com.github.exopandora.shouldersurfing.client.InputHandler;

public class ShoulderSurfingCompatInner {
    public static boolean showCrosshair() {
        Perspective current = Perspective.current();
        return current == Perspective.SHOULDER_SURFING && !InputHandler.FREE_LOOK.isDown();
    }
}
