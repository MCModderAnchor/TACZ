package com.tacz.guns.network.message;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerUnloadGun {
    private final int slot;
    private final ResourceLocation gunId;

    public ClientMessagePlayerUnloadGun(int slot, ResourceLocation gunId) {
        this.slot = slot;
        this.gunId = gunId;
    }

    public static void encode(ClientMessagePlayerUnloadGun message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.slot);
        buf.writeResourceLocation(message.gunId);
    }

    public static ClientMessagePlayerUnloadGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerUnloadGun(buf.readVarInt(), buf.readResourceLocation());
    }

    public static void handle(ClientMessagePlayerUnloadGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (!player.isAlive() || player.isSpectator()
                        || message.slot < 0 || message.slot >= 9 || player.getInventory().selected != message.slot) {
                    showCannotUnload(player);
                    return;
                }
                var stack = player.getMainHandItem();
                IGun gun = IGun.getIGunOrNull(stack);
                if (gun == null || !message.gunId.equals(gun.getGunId(stack))) {
                    showCannotUnload(player);
                    return;
                }
                var index = TimelessAPI.getCommonGunIndex(message.gunId).orElse(null);
                if (index == null) {
                    showCannotUnload(player);
                    return;
                }
                if (!index.canUnloadAmmo() || gun.useInventoryAmmo(stack)) {
                    player.displayClientMessage(Component.translatable("message.tacz.unload.unsupported"), true);
                    return;
                }
                Bolt bolt = index.getGunData().getBolt();
                long ammoBefore = getLoadedAmmoCount(gun, stack, bolt);
                if (ammoBefore == 0) {
                    player.displayClientMessage(Component.translatable("message.tacz.unload.no_ammo"), true);
                    return;
                }
                var gunDisplayId = gun.getGunDisplayId(stack);
                IGunOperator.fromLivingEntity(player).unloadAmmo();
                player.inventoryMenu.broadcastChanges();
                if (getLoadedAmmoCount(gun, stack, bolt) < ammoBefore) {
                    NetworkHandler.sendToClientPlayer(new ServerMessageGunUnload(message.gunId, gunDisplayId), player);
                } else {
                    showCannotUnload(player);
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static long getLoadedAmmoCount(IGun gun, ItemStack stack, Bolt bolt) {
        return Math.max(0L, gun.getCurrentAmmoCount(stack))
                + (bolt != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(stack) ? 1L : 0L);
    }

    private static void showCannotUnload(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.tacz.unload.cannot"), true);
    }
}
