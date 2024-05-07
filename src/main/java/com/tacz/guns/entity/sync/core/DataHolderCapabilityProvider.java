package com.tacz.guns.entity.sync.core;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DataHolderCapabilityProvider implements ICapabilitySerializable<ListTag> {
    public static final Capability<DataHolder> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    private final DataHolder holder = new DataHolder();
    private final LazyOptional<DataHolder> optional = LazyOptional.of(() -> this.holder);

    public void invalidate() {
        this.optional.invalidate();
    }

    @Override
    public ListTag serializeNBT() {
        ListTag list = new ListTag();
        this.holder.dataMap.forEach((key, entry) -> {
            if (key.save()) {
                CompoundTag keyTag = new CompoundTag();
                keyTag.putString("ClassKey", key.classKey().id().toString());
                keyTag.putString("DataKey", key.id().toString());
                keyTag.put("Value", entry.writeValue());
                list.add(keyTag);
            }
        });
        return list;
    }

    @Override
    public void deserializeNBT(ListTag listTag) {
        this.holder.dataMap.clear();
        listTag.forEach(entryTag -> {
            CompoundTag keyTag = (CompoundTag) entryTag;
            ResourceLocation classKey = ResourceLocation.tryParse(keyTag.getString("ClassKey"));
            ResourceLocation dataKey = ResourceLocation.tryParse(keyTag.getString("DataKey"));
            Tag value = keyTag.get("Value");
            SyncedClassKey<?> syncedClassKey = SyncedEntityData.instance().getClassKey(classKey);
            if (syncedClassKey == null) {
                return;
            }
            SyncedDataKey<?, ?> syncedDataKey = SyncedEntityData.instance().getKey(syncedClassKey, dataKey);
            if (syncedDataKey == null || !syncedDataKey.save()) {
                return;
            }
            DataEntry<?, ?> entry = new DataEntry<>(syncedDataKey);
            entry.readValue(value);
            this.holder.dataMap.put(syncedDataKey, entry);
        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CAPABILITY.orEmpty(cap, this.optional);
    }
}
