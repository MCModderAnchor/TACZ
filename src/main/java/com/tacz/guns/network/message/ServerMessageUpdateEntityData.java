package com.tacz.guns.network.message;

import com.tac.guns.client.sync.EntityDataManager;
import com.tac.guns.entity.sync.DataEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerMessageUpdateEntityData {

    private int entityId;
    private List<DataEntry<?, ?>> entries;

    public ServerMessageUpdateEntityData() {}

    public ServerMessageUpdateEntityData(int entityId, List<DataEntry<?, ?>> entries)
    {
        this.entityId = entityId;
        this.entries = entries;
    }

    public static void encode(ServerMessageUpdateEntityData message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.entries.size());
        message.entries.forEach(entry -> entry.write(buffer));
    }

    public static ServerMessageUpdateEntityData decode(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<DataEntry<?, ?>> entries = new ArrayList<>();
        for(int i = 0; i < size; i++)
        {
            entries.add(DataEntry.read(buffer));
        }
        return new ServerMessageUpdateEntityData(entityId, entries);
    }

    public static void handle(ServerMessageUpdateEntityData message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() -> EntityDataManager.handleSyncEntityData(message));
        supplier.get().setPacketHandled(true);
    }

    public int getEntityId()
    {
        return this.entityId;
    }

    public List<DataEntry<?, ?>> getEntries()
    {
        return this.entries;
    }
}
