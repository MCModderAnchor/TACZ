package com.tacz.guns.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface IMessage extends CustomPacketPayload {
    void handle(IPayloadContext context);
}
