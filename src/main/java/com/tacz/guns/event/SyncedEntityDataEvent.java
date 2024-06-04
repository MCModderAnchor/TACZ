package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.*;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public final class SyncedEntityDataEvent {
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (SyncedEntityData.instance().hasSyncedDataKey(event.getObject().getClass())) {
            DataHolderCapabilityProvider provider = new DataHolderCapabilityProvider();
            event.addCapability(new ResourceLocation(GunMod.MOD_ID, "synced_entity_data"), provider);
            // Don't add invalidate to server player since it's persistent
            if (!(event.getObject() instanceof ServerPlayer)) {
                event.addListener(provider::invalidate);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!event.getEntity().level().isClientSide()) {
            Entity entity = event.getTarget();
            DataHolder holder = SyncedEntityData.instance().getDataHolder(entity);
            if (holder != null) {
                List<DataEntry<?, ?>> entries = holder.gatherAll();
                entries.removeIf(entry -> !entry.getKey().syncMode().isTracking());
                if (!entries.isEmpty()) {
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new ServerMessageUpdateEntityData(entity.getId(), entries));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && !event.getLevel().isClientSide()) {
            DataHolder holder = SyncedEntityData.instance().getDataHolder(player);
            if (holder != null) {
                List<DataEntry<?, ?>> entries = holder.gatherAll();
                if (!entries.isEmpty()) {
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ServerMessageUpdateEntityData(player.getId(), entries));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        DataHolder oldHolder = SyncedEntityData.instance().getDataHolder(original);
        if (oldHolder == null) {
            return;
        }
        original.invalidateCaps();
        Player player = event.getEntity();
        DataHolder newHolder = SyncedEntityData.instance().getDataHolder(player);
        if (newHolder == null) {
            return;
        }
        Map<SyncedDataKey<?, ?>, DataEntry<?, ?>> dataMap = new HashMap<>(oldHolder.dataMap);
        if (event.isWasDeath()) {
            dataMap.entrySet().removeIf(entry -> !entry.getKey().persistent());
        }
        newHolder.dataMap = dataMap;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        SyncedEntityData instance = SyncedEntityData.instance();
        if (event.side != LogicalSide.SERVER) {
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!instance.isDirty()) {
            return;
        }
        List<Entity> dirtyEntities = instance.getDirtyEntities();
        if (dirtyEntities.isEmpty()) {
            instance.setDirty(false);
            return;
        }
        for (Entity entity : dirtyEntities) {
            DataHolder holder = instance.getDataHolder(entity);
            if (holder == null || !holder.isDirty()) {
                continue;
            }
            List<DataEntry<?, ?>> entries = holder.gatherDirty();
            if (entries.isEmpty()) {
                continue;
            }
            List<DataEntry<?, ?>> selfEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isSelf()).collect(Collectors.toList());
            if (!selfEntries.isEmpty() && entity instanceof ServerPlayer) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) entity), new ServerMessageUpdateEntityData(entity.getId(), selfEntries));
            }
            List<DataEntry<?, ?>> trackingEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isTracking()).collect(Collectors.toList());
            if (!trackingEntries.isEmpty()) {
                NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ServerMessageUpdateEntityData(entity.getId(), trackingEntries));
            }
            holder.clean();
        }
        dirtyEntities.clear();
        instance.setDirty(false);
    }
}