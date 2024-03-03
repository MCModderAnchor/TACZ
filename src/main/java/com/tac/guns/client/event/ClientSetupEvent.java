package com.tac.guns.client.event;

import com.tac.guns.client.input.FireSelectKey;
import com.tac.guns.client.input.InspectKey;
import com.tac.guns.client.input.ReloadKey;
import com.tac.guns.client.input.ShootKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(InspectKey.INSPECT_KEY);
        ClientRegistry.registerKeyBinding(ReloadKey.RELOAD_KEY);
        ClientRegistry.registerKeyBinding(ShootKey.SHOOT_KEY);
        ClientRegistry.registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY);
    }
}
