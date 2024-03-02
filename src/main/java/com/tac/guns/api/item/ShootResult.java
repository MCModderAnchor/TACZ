package com.tac.guns.api.item;

public enum ShootResult {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 冷却时间还没到
     */
    COOL_DOWN,
    /**
     * 无弹药
     */
    NO_AMMO
}
