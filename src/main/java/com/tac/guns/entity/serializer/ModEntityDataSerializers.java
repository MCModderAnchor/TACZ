package com.tac.guns.entity.serializer;

import com.tac.guns.api.gun.ReloadState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;

import javax.annotation.Nonnull;

public class ModEntityDataSerializers {
    public static final EntityDataSerializer<Long> LONG = new EntityDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, @Nonnull Long value) {
            buf.writeLong(value);
        }

        @Override
        public @Nonnull Long read(FriendlyByteBuf buf) {
            return buf.readLong();
        }

        @Override
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
}
