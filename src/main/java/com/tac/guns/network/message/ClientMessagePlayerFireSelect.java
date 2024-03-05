package com.tac.guns.network.message;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.gun.FireMode;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerFireSelect {
    public static void encode(ClientMessagePlayerFireSelect message, FriendlyByteBuf buf) {}

    public static ClientMessagePlayerFireSelect decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerFireSelect();
    }

    public static void handle(ClientMessagePlayerFireSelect message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer entity = context.getSender();
                if (entity == null) {
                    return;
                }
                FireMode fireMode = IGunOperator.fromLivingEntity(entity).fireSelect();
                entity.sendMessage(new TranslatableComponent("message.tac.fire_select.success", fireMode.name()), Util.NIL_UUID);
            });
        }
        context.setPacketHandled(true);
    }
}
