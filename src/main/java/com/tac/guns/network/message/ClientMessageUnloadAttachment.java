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

public class ClientMessageUnloadAttachment {
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageUnloadAttachment(int gunSlotIndex, AttachmentType attachmentType) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageUnloadAttachment message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageUnloadAttachment decode(FriendlyByteBuf buf) {
        return new ClientMessageUnloadAttachment(buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageUnloadAttachment message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                Inventory inventory = entity.getInventory();
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    ItemStack attachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                    if (attachmentItem != ItemStack.EMPTY && inventory.add(attachmentItem)) {
                        iGun.unloadAttachment(gunItem, message.attachmentType);
                        entity.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), entity);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
