package com.tac.guns.api.entity;

import com.tac.guns.api.gun.ShootResult;

public interface IClientPlayerGunOperator{
    ShootResult shoot();
    void draw();
    void reload();
    void inspect();
}
