package com.tacz.guns.api.entity;

public class ReloadState {
    /**
     * 没有进行换弹操作时，倒计时为 -1
     */
    public static final int NOT_RELOADING_COUNTDOWN = -1;
    /**
     * 换弹状态
     */
    protected ReloadState.StateType stateType;
    /**
     * 换弹状态的剩余时长，毫秒
     */
    protected long countDown;

    public ReloadState() {
        stateType = StateType.NOT_RELOADING;
        countDown = NOT_RELOADING_COUNTDOWN;
    }

    public ReloadState(ReloadState src) {
        stateType = src.stateType;
        countDown = src.countDown;
    }

    /**
     * @return 返回当前的换弹状态的类型。可用于判断是否正在进行换弹、换弹处在的阶段等。
     */
    public StateType getStateType() {
        return stateType;
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    /**
     * @return 如果 StateType 为 NOT_RELOADING，则返回 NOT_RELOADING_COUNTDOWN(= -1), 否则返回当前状态剩余的时长，单位为 ms 。
     */
    public long getCountDown() {
        if (stateType == StateType.NOT_RELOADING) {
            return NOT_RELOADING_COUNTDOWN;
        }
        return countDown;
    }

    public void setCountDown(long countDown) {
        this.countDown = countDown;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ReloadState reloadState) {
            return reloadState.stateType.equals(stateType) && reloadState.countDown == countDown;
        } else {
            return false;
        }
    }

    public enum StateType {
        /**
         * 表示当前玩家未进行换弹。
         */
        NOT_RELOADING,
        /**
         * 表示当前换弹状态为 正在进行空仓换弹 ，并处在填装弹药阶段。
         */
        EMPTY_RELOAD_FEEDING,
        /**
         * 表示当前换弹状态为 正在进行空仓换弹，并处在收尾阶段。
         */
        EMPTY_RELOAD_FINISHING,
        /**
         * 表示当前换弹状态为 正在进行战术快速换弹 ，并处在填装弹药阶段。
         */
        TACTICAL_RELOAD_FEEDING,
        /**
         * 表示当前换弹状态为 正在进行战术快速换弹，并处在收尾阶段。
         */
        TACTICAL_RELOAD_FINISHING;

        /**
         * 判断这个状态是否是空仓换弹过程中的其中一个阶段。包括空仓换弹的收尾阶段。
         */
        public boolean isReloadingEmpty() {
            return this == EMPTY_RELOAD_FEEDING || this == EMPTY_RELOAD_FINISHING;
        }

        /**
         * 判断这个状态是否是战术换弹过程中的其中一个阶段。包括战术换弹的收尾阶段。
         */
        public boolean isReloadingTactical() {
            return this == TACTICAL_RELOAD_FEEDING || this == TACTICAL_RELOAD_FINISHING;
        }

        /**
         * 判断这个状态是否是任意换弹过程中的其中一个阶段。包括任意换弹的收尾阶段。
         */
        public boolean isReloading() {
            return isReloadingEmpty() || isReloadingTactical();
        }

        /**
         * 判断这个状态是否是任意换弹过程中的的收尾阶段。
         */
        public boolean isReloadFinishing() {
            return this == StateType.EMPTY_RELOAD_FINISHING || this == StateType.TACTICAL_RELOAD_FINISHING;
        }
    }
}
