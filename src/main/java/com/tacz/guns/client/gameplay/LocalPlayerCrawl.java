package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerCrawl {
    private final LocalPlayer player;
    private boolean isCrawling = false;

    public LocalPlayerCrawl(LocalPlayer player) {
        this.player = player;
    }

    public void crawl(boolean isCrawl) {
        // 持枪才能按键趴下
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (player.isSpectator() || player.isPassenger() || !player.onGround()) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            this.isCrawling = isCrawl;
            if (isCrawl) {
                player.setForcedPose(Pose.SWIMMING);
            } else {
                player.setForcedPose(null);
            }
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCrawl(isCrawl));
        });
    }

    public boolean isCrawling() {
        return isCrawling;
    }
}
