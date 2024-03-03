package com.tac.guns.api.client.player;

import com.tac.guns.api.gun.ShootResult;
import net.minecraft.client.player.LocalPlayer;

public interface IClientPlayerGunOperator {
    ShootResult shoot();

    void draw();

    void reload();

    void inspect();

    void fireSelect();

    static IClientPlayerGunOperator fromLocalPlayer(LocalPlayer player) {
        return (IClientPlayerGunOperator) player;
    }
}
