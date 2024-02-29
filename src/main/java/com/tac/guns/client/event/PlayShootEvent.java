package com.tac.guns.client.event;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ShootMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayShootEvent {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && player.getMainHandItem().is(ModItems.GUN.get()) && mc.mouseHandler.isLeftPressed()) {
            ClientGunIndex gunIndex = ClientGunLoader.getGunIndex(GunItem.DEFAULT);
            BedrockGunModel gunModel = gunIndex.getGunModel();
            AnimationController controller = gunIndex.getController();
            if (gunModel != null && controller != null) {
                controller.runAnimation(0, "shoot", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.02f);
            }
            NetworkHandler.CHANNEL.sendToServer(new ShootMessage());
            SoundPlayManager.playClientSound(player, gunIndex.getSounds("shoot"), 1.0f, 0.8f);
            player.setXRot(player.getXRot() - 0.5f);
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (mc.getOverlay() != null) {
            return false;
        }
        if (mc.screen != null) {
            return false;
        }
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return mc.isWindowActive();
    }
}
