package com.tacz.guns.entity.sync.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish.
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public record SyncedDataKey<E extends Entity, T>(Pair<ResourceLocation, ResourceLocation> pairKey, ResourceLocation id,
                                                 SyncedClassKey<E> classKey, IDataSerializer<T> serializer,
                                                 Supplier<T> defaultValueSupplier, boolean save, boolean persistent,
                                                 SyncMode syncMode) {
    public static <E extends Entity, T> Builder<E, T> builder(SyncedClassKey<E> entityClass, IDataSerializer<T> serializer) {
        return new Builder<>(entityClass, serializer);
    }

    public void setValue(E entity, T value) {
        SyncedEntityData.instance().set(entity, this, value);
    }

    public T getValue(E entity) {
        return SyncedEntityData.instance().get(entity, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncedDataKey<?, ?> that = (SyncedDataKey<?, ?>) o;
        return Objects.equals(this.pairKey, that.pairKey);
    }

    @Override
    public int hashCode() {
        return this.pairKey.hashCode();
    }

    public enum SyncMode {
        /**
         * Prevents the key from being synced entirely. The data will only be available on the server.
         */
        NONE(false, false),

        /**
         * Syncs the key to all players including the player holding the data. If the entity the key
         * is bound to is not a player, only the tracking players will receive the data.
         */
        ALL(true, true),

        /**
         * Only allows the key to be synced to players who are tracking the entity. The entity holding
         * the data will not receive it on the client.
         */
        TRACKING_ONLY(true, false),

        /**
         * Only allows the key to be synced to entity holding the data. Any players tracking the entity
         * will not receive the data on their clients.
         */
        SELF_ONLY(false, true);

        final boolean tracking;
        final boolean self;

        SyncMode(boolean tracking, boolean self) {
            this.tracking = tracking;
            this.self = self;
        }

        public boolean isTracking() {
            return this.tracking;
        }

        public boolean isSelf() {
            return this.self;
        }
    }

    public static class Builder<E extends Entity, T> {
        private final SyncedClassKey<E> classKey;
        private final IDataSerializer<T> serializer;
        private ResourceLocation id;
        private Supplier<T> defaultValueSupplier;
        private boolean save = false;
        private boolean persistent = true;
        private SyncMode syncMode = SyncMode.ALL;

        private Builder(SyncedClassKey<E> classKey, IDataSerializer<T> serializer) {
            this.classKey = classKey;
            this.serializer = serializer;
        }

        public SyncedDataKey<E, T> build() {
            Validate.notNull(this.id, "Missing 'id' when building synced data key");
            Validate.notNull(this.defaultValueSupplier, "Missing 'defaultValueSupplier' when building synced data key");
            Pair<ResourceLocation, ResourceLocation> pairKey = Pair.of(this.classKey.id(), this.id);
            return new SyncedDataKey<>(pairKey, this.id, this.classKey, this.serializer, this.defaultValueSupplier, this.save, this.persistent, this.syncMode);
        }

        /**
         * Sets the id for the synced key. This is a required property.
         */
        public Builder<E, T> id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the id for the synced key using a String. This is a required property.
         */
        public Builder<E, T> id(String id) {
            this.id = new ResourceLocation(id);
            return this;
        }

        /**
         * Sets the id for the synced key using a String. This is a required property.
         * <p>
         * Please use {@link #id(String)} instead.
         */
        @Deprecated
        public Builder<E, T> key(String key) {
            return id(key);
        }

        /**
         * Sets the default value supplier for the synced key. This is a required property.
         */
        public Builder<E, T> defaultValueSupplier(Supplier<T> defaultValueSupplier) {
            this.defaultValueSupplier = defaultValueSupplier;
            return this;
        }

        /**
         * Saves this synced key to the players file. This means that the data will persist even if
         * the player reloads a world or joins back into the server.
         */
        public Builder<E, T> saveToFile() {
            this.save = true;
            return this;
        }

        /**
         * Stops this synced key from transferring over when a player dies and basically resets the
         * data back to result from the default value supplier. This only has an effect on players.
         */
        public Builder<E, T> resetOnDeath() {
            this.persistent = false;
            return this;
        }

        /**
         * The syncing method to use when sending data to clients.
         * See {@link SyncMode} for details
         */
        public Builder<E, T> syncMode(SyncMode mode) {
            this.syncMode = mode;
            return this;
        }
    }
}