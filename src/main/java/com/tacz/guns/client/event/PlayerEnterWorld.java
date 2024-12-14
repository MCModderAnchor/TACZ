package com.tacz.guns.client.event;

import com.tacz.guns.resource.PackConvertor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;


@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerEnterWorld {

    @SubscribeEvent
    public static void onPlayerEnterWorld(PlayerEvent.PlayerLoggedInEvent event) {
        File[] files = PackConvertor.FOLDER.toFile().listFiles();
        if (files != null && files.length > 0){
            event.getEntity().sendMessage(pre(new TranslatableComponent("message.tacz.convert_from_legacy.intro")), Util.NIL_UUID);
            event.getEntity().sendMessage(pre(new TranslatableComponent("message.tacz.convert_from_legacy.intro2")), Util.NIL_UUID);
            Component component = new TranslatableComponent("message.tacz.convert_from_legacy")
                    .append(new TranslatableComponent("message.tacz.convert_from_legacy.button")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tacz convert"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("message.tacz.convert_from_legacy.hover"))
            )));
            event.getEntity().sendMessage(pre(component), Util.NIL_UUID);
            event.getEntity().sendMessage(pre(new TranslatableComponent("message.tacz.convert_from_legacy.hint")), Util.NIL_UUID);
            event.getEntity().sendMessage(pre(new TranslatableComponent("message.tacz.convert_from_legacy.hide")), Util.NIL_UUID);
        }
    }

    private static Component pre(Component component) {
        return new TranslatableComponent("message.tacz.pre").append(component);
    }
}
