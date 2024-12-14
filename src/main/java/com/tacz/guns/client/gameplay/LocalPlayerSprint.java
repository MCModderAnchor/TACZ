package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import net.minecraft.client.player.LocalPlayer;

public class LocalPlayerSprint {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerSprint(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    /**
     * 根据情况返回玩家应当处于的冲刺状态，在玩家切换冲刺状态的时候调用。
     * 这里的逻辑应该严格与服务端对应，如果不对应，会出现客户端表现和服务端不符的情况。
     * （例如客户端的视觉效果是玩家在冲刺，而服务端玩家实际上没有冲刺）
     * @see com.tacz.guns.entity.shooter.LivingEntitySprint#getProcessedSprintStatus
     */
    public boolean getProcessedSprintStatus(boolean sprinting) {
        // 这里的逻辑应该严格与服务端对应，如果不对应，会出现客户端表现和服务端不符的情况。
        // （例如客户端的视觉效果是玩家在冲刺，而服务端玩家实际上没有冲刺）
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ReloadState.StateType reloadStateType = gunOperator.getSynReloadState().getStateType();
        if (gunOperator.getSynIsAiming() || (reloadStateType.isReloading() && !reloadStateType.isReloadFinishing())) {
            return false;
        } else {
            return sprinting;
        }
    }
}
