package com.tacz.guns.entity.sync.core;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHolder {
    public Map<SyncedDataKey<?, ?>, DataEntry<?, ?>> dataMap = new HashMap<>();
    private boolean dirty = false;

    @SuppressWarnings("unchecked")
    public <E extends Entity, T> boolean set(E entity, SyncedDataKey<?, ?> key, T value) {
        DataEntry<E, T> entry = (DataEntry<E, T>) this.dataMap.computeIfAbsent(key, DataEntry::new);
        if (!entry.getValue().equals(value)) {
            boolean dirty = !entity.level().isClientSide() && entry.getKey().syncMode() != SyncedDataKey.SyncMode.NONE;
            entry.setValue(value, dirty);
            this.dirty = dirty;
            return true;
        }
        return false;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <E extends Entity, T> T get(SyncedDataKey<E, T> key) {
        return (T) this.dataMap.computeIfAbsent(key, DataEntry::new).getValue();
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void clean() {
        this.dirty = false;
        this.dataMap.forEach((key, entry) -> entry.clean());
    }

    public List<DataEntry<?, ?>> gatherDirty() {
        List<DataEntry<?, ?>> entries = new ArrayList<>();
        for (DataEntry<?, ?> entry : this.dataMap.values()) {
            if (entry.isDirty() && entry.getKey().syncMode() != SyncedDataKey.SyncMode.NONE) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<DataEntry<?, ?>> gatherAll() {
        List<DataEntry<?, ?>> entries = new ArrayList<>();
        for (DataEntry<?, ?> entry : this.dataMap.values()) {
            if (entry.getKey().syncMode() != SyncedDataKey.SyncMode.NONE) {
                entries.add(entry);
            }
        }
        return entries;
    }
}
