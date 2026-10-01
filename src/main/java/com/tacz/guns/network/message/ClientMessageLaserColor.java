package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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

import java.util.HashMap;
import java.util.Map;

public class ClientMessageLaserColor implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageLaserColor> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_laser_color")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageLaserColor> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.idMapper(AttachmentType::fromId, AttachmentType::ordinal), ByteBufCodecs.INT), message -> message.colorMap,
        ByteBufCodecs.BOOL, message -> message.applyGunColor,
        ByteBufCodecs.INT, message -> message.gunColor,
        ByteBufCodecs.INT, message -> message.gunSlotIndex,
        (colorMap, applyGunColor, gunColor, gunSlotIndex) -> {
            ClientMessageLaserColor message = new ClientMessageLaserColor();
            message.colorMap.putAll(colorMap);
            message.applyGunColor = applyGunColor;
            message.gunColor = gunColor;
            message.gunSlotIndex = gunSlotIndex;
            return message;
        }
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final Map<AttachmentType, Integer> colorMap = new HashMap<>();
    private boolean applyGunColor = false;
    private int gunColor = 0;

    private int gunSlotIndex = -1;

    private ClientMessageLaserColor() {
    }

    public ClientMessageLaserColor(@NotNull ItemStack gun, int gunSlotIndex) {
        if (gun.getItem() instanceof IGun iGun) {
            for (AttachmentType type : AttachmentType.values()) {
                ItemStack attachment = iGun.getAttachment(Minecraft.getInstance().level.registryAccess(), gun, type);
                if (attachment.getItem() instanceof IAttachment iAttachment) {
                    if (iAttachment.hasCustomLaserColor(attachment)) {
                        colorMap.put(type, iAttachment.getLaserColor(attachment));
                    }
                }
            }
            if (iGun.hasCustomLaserColor(gun)) {
                this.gunColor = iGun.getLaserColor(gun);
                this.applyGunColor = true;
            }
            this.gunSlotIndex = gunSlotIndex;
        }
    }

    public static void handle(ClientMessageLaserColor message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (message.gunSlotIndex == -1) {
                return;
            }
            Inventory inventory = player.getInventory();
            ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
            IGun iGun = IGun.getIGunOrNull(gunItem);
            if (iGun != null) {
                for (var entry : message.colorMap.entrySet()) {
                    AttachmentType type = entry.getKey();
                    int color = entry.getValue();
                    CompoundTag tag = iGun.getAttachmentTag(gunItem, type);
                    if (tag != null) {
                        AttachmentItemDataAccessor.setLaserColorToTag(tag, color);
                    }
                    iGun.setAttachmentTag(gunItem, type, tag);
                }
                if (message.applyGunColor) {
                    iGun.setLaserColor(gunItem, message.gunColor);
                }
            }
        });
    }

}
