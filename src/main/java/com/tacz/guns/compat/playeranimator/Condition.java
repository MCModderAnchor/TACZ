package com.tacz.guns.compat.playeranimator;

import java.util.Locale;

public enum Condition {
    /**
     * 正常持枪
     */
    HOLD,
    /**
     * 瞄准
     */
    AIM,
    /**
     * 普通开火
     */
    NORMAL_FIRE,
    /**
     * 瞄准开火
     */
    AIM_FIRE,
    /**
     * 换弹
     */
    RELOAD,
    /**
     * 刺刀近战
     */
    MELEE;

    public boolean isType(String name) {
        return this.name().toLowerCase(Locale.ENGLISH).equals(name);
    }
}
