package com.tacz.guns.entity.sync.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;

public class DataEntry<E extends Entity, T> {
    public static final StreamCodec<RegistryFriendlyByteBuf, DataEntry<?, ?>> STREAM_CODEC = StreamCodec.of(
        (buffer, entry) -> entry.write(buffer),
        DataEntry::read
    );

    private final SyncedDataKey<E, T> key;
    private T value;
    private boolean dirty;

    public DataEntry(SyncedDataKey<E, T> key) {
        this.key = key;
        this.value = key.defaultValueSupplier().get();
    }

    public static DataEntry<?, ?> read(FriendlyByteBuf buffer) {
        SyncedDataKey<?, ?> key = SyncedEntityData.instance().getKey(buffer.readVarInt());
        Validate.notNull(key, "Synced key does not exist for id");
        DataEntry<?, ?> entry = new DataEntry<>(key);
        entry.readValue(buffer);
        return entry;
    }

    public SyncedDataKey<E, T> getKey() {
        return this.key;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value, boolean dirty) {
        this.value = value;
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void clean() {
        this.dirty = false;
    }

    public void write(FriendlyByteBuf buffer) {
        int id = SyncedEntityData.instance().getInternalId(this.key);
        buffer.writeVarInt(id);
        this.key.serializer().write(buffer, this.value);
    }

    public void readValue(FriendlyByteBuf buffer) {
        this.value = this.getKey().serializer().read(buffer);
    }

    public Tag writeValue(HolderLookup.Provider provider) {
        return this.key.serializer().write(provider, this.value);
    }

    public void readValue(HolderLookup.Provider provider, Tag nbt) {
        this.value = this.key.serializer().read(provider, nbt);
    }
}
