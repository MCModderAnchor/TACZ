package com.tac.guns.api.gun;

public enum ShootResult {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 未知原因失败
     */
    FAIL,
    /**
     * 冷却时间还没到
     */
    COOL_DOWN,
    /**
     * 无弹药
     */
    NO_AMMO
}
