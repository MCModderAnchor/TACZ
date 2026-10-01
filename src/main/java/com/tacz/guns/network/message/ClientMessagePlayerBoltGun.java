package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMessagePlayerBoltGun implements CustomPacketPayload {
    public static final ClientMessagePlayerBoltGun INSTANCE = new ClientMessagePlayerBoltGun();
    public static final CustomPacketPayload.Type<ClientMessagePlayerBoltGun> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_player_bolt_gun")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMessagePlayerBoltGun> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClientMessagePlayerBoltGun() { }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientMessagePlayerBoltGun message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer entity = (ServerPlayer) context.player();
            IGunOperator.fromLivingEntity(entity).bolt();
        });
    }
}
