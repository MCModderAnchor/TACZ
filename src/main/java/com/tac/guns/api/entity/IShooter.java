package com.tac.guns.api.entity;

public interface IShooter {
    void recordShootTime();

    long getShootTime();

    void recordDrawTime();

    long getDrawTime();

    void recordReloadTime();

    long getReloadTime();
}
