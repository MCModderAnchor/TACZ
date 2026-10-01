package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.VirtualOemAttachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ClientMessageInstallVirtualAttachment implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ClientMessageInstallVirtualAttachment> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "client_message_install_virtual_attachment"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ClientMessageInstallVirtualAttachment> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ClientMessageInstallVirtualAttachment::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    private final int gunSlotIndex;
    private final AttachmentType attachmentType;
    private final ResourceLocation attachmentId;

    public ClientMessageInstallVirtualAttachment(int gunSlotIndex, AttachmentType attachmentType, ResourceLocation attachmentId) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
        this.attachmentId = attachmentId;
    }

    public static void encode(ClientMessageInstallVirtualAttachment message, RegistryFriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
        buf.writeResourceLocation(message.attachmentId);
    }

    public static ClientMessageInstallVirtualAttachment decode(RegistryFriendlyByteBuf buf) {
        return new ClientMessageInstallVirtualAttachment(buf.readInt(), buf.readEnum(AttachmentType.class), buf.readResourceLocation());
    }

    public static void handle(ClientMessageInstallVirtualAttachment message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun == null || iGun.hasAttachmentLock(gunItem)) {
                    return;
                }
                if (!VirtualOemAttachment.isAllowedVirtualOem(iGun.getGunId(gunItem), message.attachmentId, message.attachmentType)) {
                    return;
                }
                ItemStack virtualAttachment = AttachmentItemBuilder.create().setId(message.attachmentId).build();
                IAttachment iAttachment = IAttachment.getIAttachmentOrNull(virtualAttachment);
                if (iAttachment == null || iAttachment.getType(virtualAttachment) != message.attachmentType || !iGun.allowAttachment(gunItem, virtualAttachment)) {
                    return;
                }
                ItemStack oldAttachmentItem = iGun.getAttachment(player.registryAccess(), gunItem, message.attachmentType);
                if (!oldAttachmentItem.isEmpty() && !VirtualOemAttachment.isMarked(oldAttachmentItem) && inventory.getFreeSlot() == -1) {
                    return;
                }
                VirtualOemAttachment.mark(virtualAttachment);
                ItemStack beforeRefit = gunItem.copy();
                iGun.installAttachment(player.registryAccess(), gunItem, virtualAttachment);
                GunRefitTrigger.INSTANCE.trigger(player, beforeRefit, gunItem);
                if (!oldAttachmentItem.isEmpty() && !VirtualOemAttachment.isMarked(oldAttachmentItem)) {
                    inventory.add(oldAttachmentItem);
                }
                AttachmentPropertyManager.postChangeEvent(player, gunItem);
                player.inventoryMenu.broadcastChanges();
                NetworkHandler.sendToClientPlayer(ServerMessageRefreshRefitScreen.INSTANCE, player);
            });
        }
    }
}
