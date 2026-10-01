package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.VirtualOemAttachment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessageRefitGun implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageRefitGun> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_refit_gun")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageRefitGun> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, message -> message.attachmentSlotIndex,
        ByteBufCodecs.INT, message -> message.gunSlotIndex,
        ByteBufCodecs.fromCodec(AttachmentType.CODEC), message -> message.attachmentType,
        ClientMessageRefitGun::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int attachmentSlotIndex;
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageRefitGun(int attachmentSlotIndex, int gunSlotIndex, AttachmentType attachmentType) {
        this.attachmentSlotIndex = attachmentSlotIndex;
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void handle(ClientMessageRefitGun message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                if (player == null) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack attachmentItem = inventory.getItem(message.attachmentSlotIndex);
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    // 服务端校验配件锁
                    if (iGun.hasAttachmentLock(gunItem)) {
                        return;
                    }
                    if (iGun.allowAttachment(gunItem, attachmentItem)) {
                        // 使用配件物品自身的真实类型，而非客户端传入的 attachmentType
                        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
                        if (iAttachment == null) {
                            return;
                        }
                        AttachmentType realType = iAttachment.getType(attachmentItem);
                        ItemStack oldAttachmentItem = iGun.getAttachment(player.registryAccess(), gunItem, realType);
                        ItemStack beforeRefit = gunItem.copy();
                        iGun.installAttachment(player.registryAccess(), gunItem, attachmentItem);
                        GunRefitTrigger.INSTANCE.trigger(player, beforeRefit, gunItem);
                        // 刷新配件数据
                        AttachmentPropertyManager.postChangeEvent(player, gunItem);
                        inventory.setItem(message.attachmentSlotIndex, VirtualOemAttachment.isMarked(oldAttachmentItem) ? ItemStack.EMPTY : oldAttachmentItem);
                        // 如果卸载的是扩容弹匣，吐出所有子弹
                        if (realType == AttachmentType.EXTENDED_MAG) {
                            iGun.dropAllAmmo(player, gunItem);
                        }
                        player.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(ServerMessageRefreshRefitScreen.INSTANCE, player);
                    }
                }
            });
        }
    }

}
