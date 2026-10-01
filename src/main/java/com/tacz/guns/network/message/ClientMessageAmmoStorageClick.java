package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.inventory.AmmoStorageMenu;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** A virtual entry interaction, not a vanilla inventory-slot click. */
public record ClientMessageAmmoStorageClick(int containerId, int entryId, AmmoStorageMenu.Action action) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ClientMessageAmmoStorageClick> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "client_message_ammo_storage_click"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ClientMessageAmmoStorageClick> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ClientMessageAmmoStorageClick::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    public static void encode(ClientMessageAmmoStorageClick message, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.entryId);
        buf.writeEnum(message.action);
    }

    public static ClientMessageAmmoStorageClick decode(RegistryFriendlyByteBuf buf) {
        return new ClientMessageAmmoStorageClick(buf.readVarInt(), buf.readVarInt(), buf.readEnum(AmmoStorageMenu.Action.class));
    }

    public static void handle(ClientMessageAmmoStorageClick message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
            if (player != null && player.containerMenu instanceof AmmoStorageMenu menu
                    && menu.containerId == message.containerId) {
                menu.handleInteraction(player, message.entryId, message.action);
            }
        });
    }
}
