package com.tac.guns.network.message.handshake;

import com.tac.guns.GunMod;
import com.tac.guns.api.sync.SyncedDataKey;
import com.tac.guns.entity.sync.SyncedEntityData;
import com.tac.guns.network.IMessage;
import com.tac.guns.network.LoginIndexHolder;
import com.tac.guns.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ServerMessageSyncedEntityDataMapping extends LoginIndexHolder implements IMessage<ServerMessageSyncedEntityDataMapping>{
    public static final Marker HANDSHAKE = MarkerManager.getMarker("TAC_HANDSHAKE");
    private Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap;

    public ServerMessageSyncedEntityDataMapping() {}

    private ServerMessageSyncedEntityDataMapping(Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap)
    {
        this.keyMap = keyMap;
    }

    @Override
    public void encode(ServerMessageSyncedEntityDataMapping message, FriendlyByteBuf buffer)
    {
        Set<SyncedDataKey<?, ?>> keys = SyncedEntityData.instance().getKeys();
        buffer.writeInt(keys.size());
        keys.forEach(key -> {
            int id = SyncedEntityData.instance().getInternalId(key);
            buffer.writeResourceLocation(key.classKey().id());
            buffer.writeResourceLocation(key.id());
            buffer.writeVarInt(id);
        });
    }

    @Override
    public ServerMessageSyncedEntityDataMapping decode(FriendlyByteBuf buffer)
    {
        int size = buffer.readInt();
        Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap = new HashMap<>();
        for(int i = 0; i < size; i++)
        {
            ResourceLocation classId = buffer.readResourceLocation();
            ResourceLocation keyId = buffer.readResourceLocation();
            int id = buffer.readVarInt();
            keyMap.computeIfAbsent(classId, c -> new ArrayList<>()).add(Pair.of(keyId, id));
        }
        return new ServerMessageSyncedEntityDataMapping(keyMap);
    }

    @Override
    public void handle(ServerMessageSyncedEntityDataMapping message, Supplier<NetworkEvent.Context> supplier)
    {
        GunMod.LOGGER.debug(HANDSHAKE, "Received synced key mappings from server");
        CountDownLatch block = new CountDownLatch(1);
        supplier.get().enqueueWork(() ->
        {
            if(!SyncedEntityData.instance().updateMappings(message))
            {
                supplier.get().getNetworkManager().disconnect(new TextComponent("Connection closed - [TacZ] Received unknown synced data keys."));
            }
            block.countDown();
        });
        try
        {
            block.await();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        supplier.get().setPacketHandled(true);
        NetworkHandler.HANDSHAKE_CHANNEL.reply(new Acknowledge(), supplier.get());
    }

    public Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> getKeyMap()
    {
        return this.keyMap;
    }
}
