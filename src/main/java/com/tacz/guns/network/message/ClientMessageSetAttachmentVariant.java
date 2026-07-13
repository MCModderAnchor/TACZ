package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.VariantHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ClientMessageSetAttachmentVariant {
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;
    @Nullable
    private final ResourceLocation variantId;

    public ClientMessageSetAttachmentVariant(int gunSlotIndex, AttachmentType attachmentType, @Nullable ResourceLocation variantId) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
        this.variantId = variantId;
    }

    public static void encode(ClientMessageSetAttachmentVariant message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
        buf.writeBoolean(message.variantId != null);
        if (message.variantId != null) {
            buf.writeResourceLocation(message.variantId);
        }
    }

    public static ClientMessageSetAttachmentVariant decode(FriendlyByteBuf buf) {
        int gunSlotIndex = buf.readInt();
        AttachmentType attachmentType = buf.readEnum(AttachmentType.class);
        ResourceLocation variantId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new ClientMessageSetAttachmentVariant(gunSlotIndex, attachmentType, variantId);
    }

    public static void handle(ClientMessageSetAttachmentVariant message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> handleServer(message, context.getSender()));
        }
        context.setPacketHandled(true);
    }

    private static void handleServer(ClientMessageSetAttachmentVariant message, @Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null || gun.hasAttachmentLock(gunItem)) {
            return;
        }
        ItemStack attachmentItem = gun.getAttachment(gunItem, message.attachmentType);
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (attachment == null || attachmentItem.isEmpty()) {
            return;
        }
        ResourceLocation attachmentId = attachment.getAttachmentId(attachmentItem);
        if (message.variantId == null) {
            VariantHelper.removeVariant(attachmentItem);
        } else {
            boolean valid = com.tacz.guns.api.TimelessAPI.getCommonAttachmentVariantIndex(message.variantId)
                    .map(index -> index.appliesTo(attachmentId))
                    .orElse(false);
            if (!valid) {
                return;
            }
            VariantHelper.setVariant(attachmentItem, message.variantId);
        }
        gun.installAttachment(gunItem, attachmentItem);
        AttachmentPropertyManager.postChangeEvent(player, gunItem);
        player.inventoryMenu.broadcastChanges();
        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
    }
}
