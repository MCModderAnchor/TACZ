package com.tac.guns.api.gun;

public enum ShootResult {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 未知原因失败
     */
    UNKNOWN_FAIL,
    /**
     * 射击冷却时间还没到
     */
    COOL_DOWN,
    /**
     * 无弹药
     */
    NO_AMMO,
    /**
     * 没有执行切枪逻辑
     */
    NOT_DRAW,
    /**
     * 当前物品不是枪
     */
    NOT_GUN,
    /**
     * 枪械 ID 不存在
     */
    ID_NOT_EXIST,
    /**
     * 需要手动上膛
     */
    NEED_BOLT,
    /**
     * 正处于换弹状态
     */
    IS_RELOADING,
    /**
     * 正处于切枪状态
     */
    IS_DRAWING,
    /**
     * 正处于拉拴状态
     */
    IS_BOLTING,
    /**
     * 正处于疾跑状态
     */
    IS_SPRINTING,
    /**
     * Forge 事件原因取消
     */
    FORGE_EVENT_CANCEL
}
