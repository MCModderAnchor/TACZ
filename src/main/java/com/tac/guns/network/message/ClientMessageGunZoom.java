package com.tac.guns.network.message;

import com.mojang.logging.LogUtils;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessageGunZoom {
    public static void encode(ClientMessageGunZoom message, FriendlyByteBuf buf) {
    }

    public static ClientMessageGunZoom decode(FriendlyByteBuf buf) {
        return new ClientMessageGunZoom();
    }

    public static void handle(ClientMessageGunZoom message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                ItemStack gunItem = entity.getMainHandItem();
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    ItemStack scopeItem = iGun.getAttachment(gunItem, AttachmentType.SCOPE);
                    IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
                    if (iAttachment != null) {
                        TimelessAPI.getCommonAttachmentIndex(iAttachment.getAttachmentId(scopeItem)).ifPresent(index -> {
                            int zoomNumber = iAttachment.getZoomNumber(scopeItem);
                            ++zoomNumber;
                            iAttachment.setZoomNumber(scopeItem, zoomNumber);
                            iGun.setAttachment(gunItem, scopeItem);
                        });
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
