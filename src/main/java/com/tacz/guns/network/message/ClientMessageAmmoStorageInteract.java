package com.tacz.guns.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.core.BlockPos;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class ClientMessageAmmoStorageInteract implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ClientMessageAmmoStorageInteract> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "client_message_ammo_storage_interact"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ClientMessageAmmoStorageInteract> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buf, message) -> encode(message, buf), ClientMessageAmmoStorageInteract::decode);
    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }

    private static final String LAST_PICKUP_TAG = "TacAmmoStorageLastPickup";
    private static final String LAST_ACCUMULATE_TAG = "TacAmmoStorageLastAccumulate";
    private static final String ACCUMULATE_AMMO_TAG = "TacAmmoStorageAccumulateAmmo";
    private static final String ACCUMULATE_COUNT_TAG = "TacAmmoStorageAccumulateCount";
    private static final long PICKUP_COOLDOWN_TICKS = 5L;
    private static final long ACCUMULATE_WINDOW_TICKS = 20L;

    private final BlockPos pos;

    public ClientMessageAmmoStorageInteract(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ClientMessageAmmoStorageInteract message, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
    }

    public static ClientMessageAmmoStorageInteract decode(RegistryFriendlyByteBuf buf) {
        return new ClientMessageAmmoStorageInteract(buf.readBlockPos());
    }

    public static void handle(ClientMessageAmmoStorageInteract message, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                if (player == null || player.isSpectator()) {
                    return;
                }
                long gameTime = player.level().getGameTime();
                long lastPickup = player.getPersistentData().getLong(LAST_PICKUP_TAG);
                if (lastPickup > 0L && gameTime - lastPickup < PICKUP_COOLDOWN_TICKS) {
                    return;
                }
                if (player.distanceToSqr(message.pos.getX() + 0.5D, message.pos.getY() + 0.5D, message.pos.getZ() + 0.5D) > 36.0D) {
                    return;
                }
                ItemStack gunStack = player.getMainHandItem();
                if (!(gunStack.getItem() instanceof IGun gun)) {
                    return;
                }
                ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack))
                        .map(index -> index.getGunData().getAmmoId())
                        .orElse(null);
                if (ammoId == null) {
                    return;
                }
                BlockEntity blockEntity = player.level().getBlockEntity(message.pos);
                if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
                    return;
                }
                int amount = AmmoStorageHelper.getStackSize(AmmoStorageHelper.createAmmoStack(ammoId, 1));
                ItemStack extracted = storage.extractAmmo(ammoId, amount, false);
                if (extracted.isEmpty()) {
                    player.getPersistentData().putLong(LAST_PICKUP_TAG, gameTime);
                    player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.no_gun_stock"), true);
                    return;
                }
                player.getPersistentData().putLong(LAST_PICKUP_TAG, gameTime);
                int count = extracted.getCount();
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                int accumulatedCount = getAccumulatedCount(player, ammoId, count, gameTime);
                player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.picked", AmmoStorageHelper.createAmmoStack(ammoId, 1).getHoverName(), accumulatedCount), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8F, 1.0F);
            });
        }
    }

    private static int getAccumulatedCount(ServerPlayer player, ResourceLocation ammoId, int count, long gameTime) {
        String ammoKey = ammoId.toString();
        long lastAccumulate = player.getPersistentData().getLong(LAST_ACCUMULATE_TAG);
        String lastAmmoKey = player.getPersistentData().getString(ACCUMULATE_AMMO_TAG);
        int accumulated = count;
        if (lastAccumulate > 0L && gameTime - lastAccumulate <= ACCUMULATE_WINDOW_TICKS && ammoKey.equals(lastAmmoKey)) {
            accumulated += player.getPersistentData().getInt(ACCUMULATE_COUNT_TAG);
        }
        player.getPersistentData().putLong(LAST_ACCUMULATE_TAG, gameTime);
        player.getPersistentData().putString(ACCUMULATE_AMMO_TAG, ammoKey);
        player.getPersistentData().putInt(ACCUMULATE_COUNT_TAG, accumulated);
        return accumulated;
    }
}
