package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.config.common.GunConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 仅在服务端确认本次主动卸弹实际减少了枪内弹药后发送给操作者。 */
public class ServerMessageGunUnload {
    private static final ResourceLocation UNLOAD_SOUND = new ResourceLocation(GunMod.MOD_ID,
            "attachments/attachment_general_uninstall");

    public static void encode(ServerMessageGunUnload message, FriendlyByteBuf buf) {
    }

    public static ServerMessageGunUnload decode(FriendlyByteBuf buf) {
        return new ServerMessageGunUnload();
    }

    public static void handle(ServerMessageGunUnload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(ServerMessageGunUnload::playSound);
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void playSound() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SoundPlayManager.playClientSound(player, UNLOAD_SOUND, 1.0F, 1.0F,
                    GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        }
    }
}
