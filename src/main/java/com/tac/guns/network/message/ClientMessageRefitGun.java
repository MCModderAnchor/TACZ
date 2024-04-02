package com.tac.guns.network.message;

import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IGun;
import com.tac.guns.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessageRefitGun {
    private final int attachmentSlotIndex;
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageRefitGun(int attachmentSlotIndex, int gunSlotIndex, AttachmentType attachmentType) {
        this.attachmentSlotIndex = attachmentSlotIndex;
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageRefitGun message, FriendlyByteBuf buf) {
        buf.writeInt(message.attachmentSlotIndex);
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageRefitGun decode(FriendlyByteBuf buf) {
        return new ClientMessageRefitGun(buf.readInt(), buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageRefitGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                Inventory inventory = entity.getInventory();
                ItemStack attachmentItem = inventory.getItem(message.attachmentSlotIndex);
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    if (iGun.allowAttachment(gunItem, attachmentItem)) {
                        ItemStack oldAttachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                        iGun.installAttachment(gunItem, attachmentItem);
                        inventory.setItem(message.attachmentSlotIndex, oldAttachmentItem);
                        entity.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), entity);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
