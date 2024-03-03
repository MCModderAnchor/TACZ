package com.tac.guns.api.gun;

import com.tac.guns.resource.pojo.data.GunReloadData;

public class ReloadState {
    public static final int NOT_RELOADING_COUNTDOWN = -1;
    private long reloadTimestamp;
    private ReloadState.StateType stateType;
    private long countDown;

    public ReloadState(){
        reloadTimestamp = -1L;
        stateType = StateType.NOT_RELOADING;
        countDown = NOT_RELOADING_COUNTDOWN;
    }

    public ReloadState(ReloadState src){
        reloadTimestamp = src.reloadTimestamp;
        stateType = src.stateType;
        countDown = src.countDown;
    }

    public void startReloadEmpty(){
        reloadTimestamp = System.currentTimeMillis();
        stateType = StateType.EMPTY_RELOAD_FEEDING;
    }

    public void startReloadNormal(){
        reloadTimestamp = System.currentTimeMillis();
        stateType = StateType.NORMAL_RELOAD_FEEDING;
    }

    public long getReloadTimestamp() {
        return reloadTimestamp;
    }

    public boolean isReloading(){
        return stateType != StateType.NOT_RELOADING;
    }

    public boolean isReloadFinished(){
        return stateType != StateType.EMPTY_RELOAD_FEEDING && stateType != StateType.NORMAL_RELOAD_FEEDING;
    }

    /**
     * @return 返回当前的换弹状态的类型。可用于判断是否正在进行换弹、换弹处在的阶段等。
     */
    public StateType getStateType() {
        return stateType;
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

    public void setReloadTimestamp(long reloadTimestamp) {
        this.reloadTimestamp = reloadTimestamp;
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    public void setCountDown(long countDown) {
        this.countDown = countDown;
    }

    public void tick(GunReloadData reloadData){
        if(reloadTimestamp < 0){
            return;
        }
        long progressTime = System.currentTimeMillis() - reloadTimestamp;
        if(stateType.isReloadEmpty()){
            long magFedTime = (long) (reloadData.getEmptyMagFedTime() * 1000);
            long finishingTime = (long) (reloadData.getEmptyReloadTime() * 1000);
            if(progressTime < magFedTime){
                stateType = StateType.EMPTY_RELOAD_FEEDING;
                countDown = magFedTime - progressTime;
            }else if(progressTime < finishingTime){
                stateType = StateType.EMPTY_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            }else {
                stateType = StateType.NOT_RELOADING;
                countDown = NOT_RELOADING_COUNTDOWN;
            }
        }else if(stateType.isReloadNormal()){
            long magFedTime = (long) (reloadData.getNormalMagFedTime() * 1000);
            long finishingTime = (long) (reloadData.getNormalReloadTime() * 1000);
            if(progressTime < magFedTime){
                stateType = StateType.NORMAL_RELOAD_FEEDING;
                countDown = magFedTime - progressTime;
            }else if(progressTime < finishingTime){
                stateType = StateType.NORMAL_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            }else {
                stateType = StateType.NOT_RELOADING;
                countDown = NOT_RELOADING_COUNTDOWN;
            }
        }else {
            stateType = StateType.NOT_RELOADING;
            countDown = NOT_RELOADING_COUNTDOWN;
        }
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof ReloadState reloadState){
            return reloadState.stateType.equals(stateType) && reloadState.countDown == countDown && reloadState.reloadTimestamp == reloadTimestamp;
        }else {
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
        NORMAL_RELOAD_FEEDING,
        /**
         * 表示当前换弹状态为 正在进行战术快速换弹，并处在收尾阶段。
         */
        NORMAL_RELOAD_FINISHING;

        public boolean isReloadEmpty(){
            return this == EMPTY_RELOAD_FEEDING || this == EMPTY_RELOAD_FINISHING;
        }

        public boolean isReloadNormal(){
            return this == NORMAL_RELOAD_FEEDING || this == NORMAL_RELOAD_FINISHING;
        }
    }
}
