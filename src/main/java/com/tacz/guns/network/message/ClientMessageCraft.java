package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessageCraft implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageCraft> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_craft")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessageCraft> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, message -> message.recipeId,
        ByteBufCodecs.INT, message -> message.menuId,
        ClientMessageCraft::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceLocation recipeId;
    private final int menuId;

    public ClientMessageCraft(ResourceLocation recipeId, int menuId) {
        this.recipeId = recipeId;
        this.menuId = menuId;
    }

    public static void handle(ClientMessageCraft message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer entity = (ServerPlayer) context.player();
            if (entity.containerMenu.containerId == message.menuId && entity.containerMenu instanceof GunSmithTableMenu menu) {
                menu.doCraft(message.recipeId, entity);
            }
        });
    }
}
