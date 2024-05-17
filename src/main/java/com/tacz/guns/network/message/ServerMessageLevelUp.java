package com.tacz.guns.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageLevelUp {
    private final ItemStack gun;
    private final int level;

    public ServerMessageLevelUp(ItemStack gun, int level) {
        this.gun = gun;
        this.level = level;
    }

    public static void encode(ServerMessageLevelUp message, FriendlyByteBuf buf) {
        buf.writeItemStack(message.gun, true);
        buf.writeInt(message.level);
    }

    public static ServerMessageLevelUp decode(FriendlyByteBuf buf) {
        ItemStack gun = buf.readItem();
        int level = buf.readInt();
        return new ServerMessageLevelUp(gun, level);
    }

    public static void handle(ServerMessageLevelUp message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onLevelUp(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onLevelUp(ServerMessageLevelUp message) {
        int level = message.getLevel();
        ItemStack gun = message.getGun();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // TODO 在完成了枪械升级逻辑后，解封下面的代码
                /*
                if (GunLevelManager.DAMAGE_UP_LEVELS.contains(level)) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.damage_up")));
                } else if (level >= GunLevelManager.MAX_LEVEL) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.final_level")));
                } else {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.level_up")));
                }*/
    }

    public ItemStack getGun() {
        return this.gun;
    }

    public int getLevel() {
        return this.level;
    }
}
