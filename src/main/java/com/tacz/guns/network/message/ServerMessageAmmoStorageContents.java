package com.tacz.guns.network.message;

import com.tacz.guns.inventory.AmmoStorageEntry;
import com.tacz.guns.inventory.AmmoStorageMenu;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ServerMessageAmmoStorageContents(int containerId, int capacityStacks, long usedStacks,
                                               List<AmmoStorageEntry> entries) {
    public ServerMessageAmmoStorageContents {
        entries = List.copyOf(entries);
    }

    public static void encode(ServerMessageAmmoStorageContents message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.capacityStacks);
        buf.writeVarLong(message.usedStacks);
        buf.writeVarInt(message.entries.size());
        for (AmmoStorageEntry entry : message.entries) {
            buf.writeVarInt(entry.id());
            buf.writeItem(entry.sample());
            buf.writeVarLong(entry.count());
            buf.writeVarLong(entry.remainingCapacity());
        }
    }

    public static ServerMessageAmmoStorageContents decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int capacityStacks = buf.readVarInt();
        long usedStacks = buf.readVarLong();
        int size = buf.readVarInt();
        if (capacityStacks < 0 || usedStacks < 0 || size < 0 || size > AmmoStorageMenu.STORAGE_SLOT_COUNT) {
            throw new DecoderException("Invalid ammo storage entry count: " + size);
        }
        List<AmmoStorageEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int id = buf.readVarInt();
            ItemStack sample = buf.readItem();
            long count = buf.readVarLong();
            long remaining = buf.readVarLong();
            if (id < 0 || sample.isEmpty() || count <= 0 || remaining < 0) {
                throw new DecoderException("Invalid ammo storage entry");
            }
            entries.add(new AmmoStorageEntry(id, sample, count, remaining));
        }
        return new ServerMessageAmmoStorageContents(containerId, capacityStacks, usedStacks, entries);
    }

    public static void handle(ServerMessageAmmoStorageContents message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> updateContents(message));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateContents(ServerMessageAmmoStorageContents message) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof AmmoStorageMenu menu
                && menu.containerId == message.containerId) {
            menu.setContents(message.capacityStacks, message.usedStacks, message.entries);
        }
    }
}
