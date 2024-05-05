package com.tacz.guns.client.sync;

import com.tacz.guns.entity.sync.DataEntry;
import com.tacz.guns.entity.sync.SyncedEntityData;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

public class EntityDataManager {
    public static void handleSyncEntityData(ServerMessageUpdateEntityData message) {
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Entity entity = level.getEntity(message.getEntityId());
        if (entity == null)
            return;

        List<DataEntry<?, ?>> entries = message.getEntries();
        entries.forEach(entry -> updateClientEntry(entity, entry));
    }

    public static <E extends Entity, T> void updateClientEntry(Entity entity, DataEntry<E, T> entry) {
        SyncedEntityData.instance().set(entity, entry.getKey(), entry.getValue());
    }
}
