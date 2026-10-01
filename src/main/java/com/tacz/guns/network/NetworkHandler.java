package com.tacz.guns.network;

import com.tacz.guns.GunMod;
import com.tacz.guns.network.message.*;
import com.tacz.guns.network.message.event.*;
import com.tacz.guns.network.message.handshake.Acknowledge;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static final String VERSION = "1.2.0-port-trial.1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(ClientMessageAmmoStorageClick.TYPE, ClientMessageAmmoStorageClick.STREAM_CODEC, ClientMessageAmmoStorageClick::handle);
        registrar.playToServer(ClientMessageAmmoStorageInteract.TYPE, ClientMessageAmmoStorageInteract.STREAM_CODEC, ClientMessageAmmoStorageInteract::handle);
        registrar.playToServer(ClientMessageCraft.TYPE, ClientMessageCraft.STREAM_CODEC, ClientMessageCraft::handle);
        registrar.playToServer(ClientMessageInstallVirtualAttachment.TYPE, ClientMessageInstallVirtualAttachment.STREAM_CODEC, ClientMessageInstallVirtualAttachment::handle);
        registrar.playToServer(ClientMessageLaserColor.TYPE, ClientMessageLaserColor.STREAM_CODEC, ClientMessageLaserColor::handle);
        registrar.playToServer(ClientMessagePlayerAim.TYPE, ClientMessagePlayerAim.STREAM_CODEC, ClientMessagePlayerAim::handle);
        registrar.playToServer(ClientMessagePlayerBoltGun.TYPE, ClientMessagePlayerBoltGun.STREAM_CODEC, ClientMessagePlayerBoltGun::handle);
        registrar.playToServer(ClientMessagePlayerCancelReload.TYPE, ClientMessagePlayerCancelReload.STREAM_CODEC, ClientMessagePlayerCancelReload::handle);
        registrar.playToServer(ClientMessagePlayerCrawl.TYPE, ClientMessagePlayerCrawl.STREAM_CODEC, ClientMessagePlayerCrawl::handle);
        registrar.playToServer(ClientMessagePlayerDrawGun.TYPE, ClientMessagePlayerDrawGun.STREAM_CODEC, ClientMessagePlayerDrawGun::handle);
        registrar.playToServer(ClientMessagePlayerFireSelect.TYPE, ClientMessagePlayerFireSelect.STREAM_CODEC, ClientMessagePlayerFireSelect::handle);
        registrar.playToServer(ClientMessagePlayerMelee.TYPE, ClientMessagePlayerMelee.STREAM_CODEC, ClientMessagePlayerMelee::handle);
        registrar.playToServer(ClientMessagePlayerReloadGun.TYPE, ClientMessagePlayerReloadGun.STREAM_CODEC, ClientMessagePlayerReloadGun::handle);
        registrar.playToServer(ClientMessagePlayerShoot.TYPE, ClientMessagePlayerShoot.STREAM_CODEC, ClientMessagePlayerShoot::handle);
        registrar.playToServer(ClientMessagePlayerUnloadGun.TYPE, ClientMessagePlayerUnloadGun.STREAM_CODEC, ClientMessagePlayerUnloadGun::handle);
        registrar.playToServer(ClientMessagePlayerZoom.TYPE, ClientMessagePlayerZoom.STREAM_CODEC, ClientMessagePlayerZoom::handle);
        registrar.playToServer(ClientMessageRefitGun.TYPE, ClientMessageRefitGun.STREAM_CODEC, ClientMessageRefitGun::handle);
        registrar.playToServer(ClientMessageSetAttachmentVariant.TYPE, ClientMessageSetAttachmentVariant.STREAM_CODEC, ClientMessageSetAttachmentVariant::handle);
        registrar.playToServer(ClientMessageSetSlotAdapter.TYPE, ClientMessageSetSlotAdapter.STREAM_CODEC, ClientMessageSetSlotAdapter::handle);
        registrar.playToServer(ClientMessageSyncBaseTimestamp.TYPE, ClientMessageSyncBaseTimestamp.STREAM_CODEC, ClientMessageSyncBaseTimestamp::handle);
        registrar.playToServer(ClientMessageUnloadAttachment.TYPE, ClientMessageUnloadAttachment.STREAM_CODEC, ClientMessageUnloadAttachment::handle);
        registrar.playToClient(ServerMessageAmmoStorageContents.TYPE, ServerMessageAmmoStorageContents.STREAM_CODEC, ServerMessageAmmoStorageContents::handle);
        registrar.playToClient(ServerMessageBulletHitBlock.TYPE, ServerMessageBulletHitBlock.STREAM_CODEC, ServerMessageBulletHitBlock::handle);
        registrar.playToClient(ServerMessageCraft.TYPE, ServerMessageCraft.STREAM_CODEC, ServerMessageCraft::handle);
        registrar.playToClient(ServerMessageGunUnload.TYPE, ServerMessageGunUnload.STREAM_CODEC, ServerMessageGunUnload::handle);
        registrar.playToClient(ServerMessageLevelUp.TYPE, ServerMessageLevelUp.STREAM_CODEC, ServerMessageLevelUp::handle);
        registrar.playToClient(ServerMessageRefreshRefitScreen.TYPE, ServerMessageRefreshRefitScreen.STREAM_CODEC, ServerMessageRefreshRefitScreen::handle);
        registrar.playToClient(ServerMessageSound.TYPE, ServerMessageSound.STREAM_CODEC, ServerMessageSound::handle);
        registrar.playToClient(ServerMessageSwapItem.TYPE, ServerMessageSwapItem.STREAM_CODEC, ServerMessageSwapItem::handle);
        registrar.playToClient(ServerMessageSyncBaseTimestamp.TYPE, ServerMessageSyncBaseTimestamp.STREAM_CODEC, ServerMessageSyncBaseTimestamp::handle);
        registrar.playToClient(ServerMessageSyncGunPack.TYPE, ServerMessageSyncGunPack.STREAM_CODEC, ServerMessageSyncGunPack::handle);
        registrar.playToClient(ServerMessageUpdateEntityData.TYPE, ServerMessageUpdateEntityData.STREAM_CODEC, ServerMessageUpdateEntityData::handle);
        registrar.playToClient(ServerMessageGunDraw.TYPE, ServerMessageGunDraw.STREAM_CODEC, ServerMessageGunDraw::handle);
        registrar.playToClient(ServerMessageGunFinishReload.TYPE, ServerMessageGunFinishReload.STREAM_CODEC, ServerMessageGunFinishReload::handle);
        registrar.playToClient(ServerMessageGunFire.TYPE, ServerMessageGunFire.STREAM_CODEC, ServerMessageGunFire::handle);
        registrar.playToClient(ServerMessageGunFirePost.TYPE, ServerMessageGunFirePost.STREAM_CODEC, ServerMessageGunFirePost::handle);
        registrar.playToClient(ServerMessageGunFireSelect.TYPE, ServerMessageGunFireSelect.STREAM_CODEC, ServerMessageGunFireSelect::handle);
        registrar.playToClient(ServerMessageGunHurt.TYPE, ServerMessageGunHurt.STREAM_CODEC, ServerMessageGunHurt::handle);
        registrar.playToClient(ServerMessageGunKill.TYPE, ServerMessageGunKill.STREAM_CODEC, ServerMessageGunKill::handle);
        registrar.playToClient(ServerMessageGunMelee.TYPE, ServerMessageGunMelee.STREAM_CODEC, ServerMessageGunMelee::handle);
        registrar.playToClient(ServerMessageGunReload.TYPE, ServerMessageGunReload.STREAM_CODEC, ServerMessageGunReload::handle);
        registrar.playToClient(ServerMessageGunShoot.TYPE, ServerMessageGunShoot.STREAM_CODEC, ServerMessageGunShoot::handle);



        final PayloadRegistrar handshakeRegistrar = event.registrar(VERSION).executesOn(HandlerThread.NETWORK);
        handshakeRegistrar.configurationToServer(Acknowledge.TYPE, Acknowledge.STREAM_CODEC, Acknowledge::handle);
        handshakeRegistrar.configurationToClient(ServerMessageSyncedEntityDataMapping.TYPE, ServerMessageSyncedEntityDataMapping.STREAM_CODEC, ServerMessageSyncedEntityDataMapping::handle);
    }

    @SubscribeEvent
    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new Task());
    }

    public static void sendToClientPlayer(CustomPacketPayload message, Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer) player, message);
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, CustomPacketPayload message) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(centerEntity, message);
    }

    public static void sendToAllPlayers(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendToTrackingEntity(CustomPacketPayload message, final Entity centerEntity) {
        PacketDistributor.sendToPlayersTrackingEntity(centerEntity, message);
    }

    public static void sendToDimension(CustomPacketPayload message, final Entity centerEntity) {
        Level level = centerEntity.level();
        PacketDistributor.sendToPlayersInDimension((ServerLevel) level, message);
    }

    public static class Task implements ICustomConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(
            ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "configuration")
        );

        @Override
        public @NotNull ConfigurationTask.Type type() {
            return TYPE;
        }

        @Override
        public void run(@NotNull Consumer<CustomPacketPayload> consumer) {
            consumer.accept(new ServerMessageSyncedEntityDataMapping());
        }
    }
}
