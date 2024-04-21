package com.tac.guns.event;

import com.tac.guns.GunMod;
import com.tac.guns.api.event.GunLevelEvent;
import com.tac.guns.api.item.IGun;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ServerMessageLevelUp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunLevel {
    private static final String GUN_LEVEL_TAG = "GunLevel";

    @SubscribeEvent
    public static void onLevelEvent(GunLevelEvent.Post event) {
        Player player = event.getPlayer();
        ItemStack gun = player.getMainHandItem();
        if (gun.getItem() instanceof IGun) {
            CompoundTag tag = gun.getOrCreateTag();
            int level = tag.getInt(GUN_LEVEL_TAG);
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToClientPlayer(new ServerMessageLevelUp(gun, level), serverPlayer);
                player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.70F, 1.0F);
            }
        }
    }
}
