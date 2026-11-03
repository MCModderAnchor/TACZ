package com.tacz.guns.network.message;

import com.tacz.guns.client.sound.SoundPlayManager;
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
    private final ResourceLocation gunId;
    private final ResourceLocation gunDisplayId;

    public ServerMessageGunUnload(ResourceLocation gunId, ResourceLocation gunDisplayId) {
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
    }

    public static void encode(ServerMessageGunUnload message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.gunId);
        buf.writeResourceLocation(message.gunDisplayId);
    }

    public static ServerMessageGunUnload decode(FriendlyByteBuf buf) {
        return new ServerMessageGunUnload(buf.readResourceLocation(), buf.readResourceLocation());
    }

    public static void handle(ServerMessageGunUnload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> playSound(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void playSound(ServerMessageGunUnload message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SoundPlayManager.playUnloadSound(player, message.gunId, message.gunDisplayId);
        }
    }
}
