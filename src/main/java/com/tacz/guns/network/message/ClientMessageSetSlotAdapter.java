package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.SlotAdapterHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ClientMessageSetSlotAdapter implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ClientMessageSetSlotAdapter> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "client_message_set_slot_adapter"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ClientMessageSetSlotAdapter> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ClientMessageSetSlotAdapter::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    private final int gunSlotIndex;
    private final AttachmentType attachmentType;
    @Nullable
    private final ResourceLocation adapterId;

    public ClientMessageSetSlotAdapter(int gunSlotIndex, AttachmentType attachmentType, @Nullable ResourceLocation adapterId) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
        this.adapterId = adapterId;
    }

    public static void encode(ClientMessageSetSlotAdapter message, RegistryFriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
        buf.writeBoolean(message.adapterId != null);
        if (message.adapterId != null) {
            buf.writeResourceLocation(message.adapterId);
        }
    }

    public static ClientMessageSetSlotAdapter decode(RegistryFriendlyByteBuf buf) {
        int gunSlotIndex = buf.readInt();
        AttachmentType attachmentType = buf.readEnum(AttachmentType.class);
        ResourceLocation adapterId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new ClientMessageSetSlotAdapter(gunSlotIndex, attachmentType, adapterId);
    }

    public static void handle(ClientMessageSetSlotAdapter message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> handleServer(message, (net.minecraft.server.level.ServerPlayer) context.player()));
        }
    }

    private static void handleServer(ClientMessageSetSlotAdapter message, @Nullable ServerPlayer player) {
        if (player == null || message.attachmentType == AttachmentType.NONE) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (message.gunSlotIndex < 0 || message.gunSlotIndex >= inventory.getContainerSize()) {
            return;
        }
        ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null || gun.hasAttachmentLock(gunItem) || !gun.allowAttachmentType(gunItem, message.attachmentType)) {
            return;
        }

        ItemStack attachmentItem = gun.getAttachment(player.registryAccess(), gunItem, message.attachmentType);
        ResourceLocation attachmentId = null;
        if (!attachmentItem.isEmpty()) {
            IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentItem);
            if (attachment == null || attachment.getType(attachmentItem) != message.attachmentType) {
                return;
            }
            attachmentId = attachment.getAttachmentId(attachmentItem);
        }

        ItemStack beforeRefit = gunItem.copy();
        if (message.adapterId == null) {
            if (attachmentId != null && !SlotAdapterHelper.allowsDirectAttachment(gunItem, attachmentId)) {
                return;
            }
            SlotAdapterHelper.removeSlotAdapter(gunItem, message.attachmentType);
        } else {
            ResourceLocation currentAttachmentId = attachmentId;
            boolean validSelection = SlotAdapterHelper.getAvailableSlotAdapters(gunItem, message.attachmentType).stream()
                    .filter(entry -> entry.getKey().equals(message.adapterId))
                    .anyMatch(entry -> currentAttachmentId == null || entry.getValue().allowsAttachment(currentAttachmentId));
            if (!validSelection) {
                return;
            }
            SlotAdapterHelper.setSlotAdapter(gunItem, message.attachmentType, message.adapterId);
        }

        AttachmentPropertyManager.postChangeEvent(player, gunItem);
        GunRefitTrigger.INSTANCE.trigger(player, beforeRefit, gunItem);
        player.inventoryMenu.broadcastChanges();
        NetworkHandler.sendToClientPlayer(ServerMessageRefreshRefitScreen.INSTANCE, player);
    }
}
