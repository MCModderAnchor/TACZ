package com.tac.guns.client.movement;

import com.tac.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.player.LocalPlayer;

public class RecoilHandler {
    private static final SecondOrderDynamics RECOIL_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static long shootTimeStamp = -1L;
    private static float xRotO = 0;

    public static void onShoot(LocalPlayer player) {
        RECOIL_DYNAMICS.update(0);
        shootTimeStamp = System.currentTimeMillis();
        xRotO = 0;
    }

    public static void onHandle(LocalPlayer player) {
        long timeTotal = System.currentTimeMillis() - shootTimeStamp;
        if (timeTotal < 600) {
            float result;
            // 分段函数
            if (timeTotal < 50) {
                result = 0.1f * timeTotal;
            } else if (timeTotal < 400) {
                result = 6f - 0.02f * timeTotal;
            } else {
                result = -7.5f + 0.015f * timeTotal;
            }
            float update = RECOIL_DYNAMICS.update(result);
            player.setXRot(player.getXRot() - (update - xRotO));
            xRotO = update;
        }
    }
}
