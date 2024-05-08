package com.tacz.guns.network.message;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.item.builder.AmmoItemBuilder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
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
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack attachmentItem = inventory.getItem(message.attachmentSlotIndex);
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    if (iGun.allowAttachment(gunItem, attachmentItem)) {
                        ItemStack oldAttachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                        iGun.installAttachment(gunItem, attachmentItem);
                        inventory.setItem(message.attachmentSlotIndex, oldAttachmentItem);
                        // 如果卸载的是扩容弹匣，吐出所有子弹
                        if (message.attachmentType == AttachmentType.EXTENDED_MAG) {
                            dropAllAmmo(player, iGun, gunItem);
                        }
                        player.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static void dropAllAmmo(Player player, IGun iGun, ItemStack gunItem) {
        int ammoCount = iGun.getCurrentAmmoCount(gunItem);
        if (ammoCount <= 0) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            ResourceLocation ammoId = index.getGunData().getAmmoId();
            // 创造模式，只改变子弹总数，不进行任何卸载弹药逻辑
            if (player.isCreative()) {
                int maxAmmCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData());
                iGun.setCurrentAmmoCount(gunItem, maxAmmCount);
                return;
            }
            TimelessAPI.getCommonAmmoIndex(ammoId).ifPresent(ammoIndex -> {
                int stackSize = ammoIndex.getStackSize();
                int tmpAmmoCount = ammoCount;
                int roundCount = tmpAmmoCount / (stackSize + 1);
                for (int i = 0; i <= roundCount; i++) {
                    int count = Math.min(tmpAmmoCount, stackSize);
                    ItemStack ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
                    ItemHandlerHelper.giveItemToPlayer(player, ammoItem);
                    tmpAmmoCount -= stackSize;
                }
                iGun.setCurrentAmmoCount(gunItem, 0);
            });
        });
    }
}
