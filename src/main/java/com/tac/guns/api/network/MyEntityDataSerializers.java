package com.tac.guns.api.network;

import com.tac.guns.api.gun.ReloadState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

import javax.annotation.Nonnull;

public class MyEntityDataSerializers {
    public static final EntityDataSerializer<Long> LONG = new EntityDataSerializer<>() {
        public void write(FriendlyByteBuf buf, @Nonnull Long value) {
            buf.writeLong(value);
        }

        public @Nonnull Long read(FriendlyByteBuf buf) {
            return buf.readLong();
        }

        public @Nonnull Long copy(@Nonnull Long value) {
            return value;
        }
    };

    public static final EntityDataSerializer<ReloadState> RELOAD_STATE = new EntityDataSerializer<>() {
        @Override
        public void write(@Nonnull FriendlyByteBuf pBuffer, @Nonnull ReloadState pValue) {
            pBuffer.writeInt(pValue.getStateType().ordinal());
            pBuffer.writeLong(pValue.getCountDown());
        }

        @Override
        public @Nonnull ReloadState read(@Nonnull FriendlyByteBuf pBuffer) {
            ReloadState reloadState = new ReloadState();
            reloadState.setStateType(ReloadState.StateType.values()[pBuffer.readInt()]);
            reloadState.setCountDown(pBuffer.readLong());
            return reloadState;
        }

        @Override
        public @Nonnull ReloadState copy(@Nonnull ReloadState pValue) {
            return new ReloadState(pValue);
        }
    };

    static {
        EntityDataSerializers.registerSerializer(LONG);
        EntityDataSerializers.registerSerializer(RELOAD_STATE);
    }
}
