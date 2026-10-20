package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ServerMessageRefreshRefitScreen implements CustomPacketPayload {
    public static final ServerMessageRefreshRefitScreen INSTANCE = new ServerMessageRefreshRefitScreen();
    public static final CustomPacketPayload.Type<ServerMessageRefreshRefitScreen> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_refresh_refit_screen")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerMessageRefreshRefitScreen> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ServerMessageRefreshRefitScreen() { }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerMessageRefreshRefitScreen message, IPayloadContext context) {
        context.enqueueWork(ServerMessageRefreshRefitScreen::updateScreen);
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateScreen() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().screen instanceof GunRefitScreen screen) {
            screen.init();
            // 刷新配件数据，客户端的
            AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());
        }
    }
}
